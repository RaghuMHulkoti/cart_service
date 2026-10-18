package com.learning.cart_service.core.usecase;

import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.core.domain.Offer;
import com.learning.cart_service.core.service.cart.CartService;
import com.learning.cart_service.core.service.cart.CartServiceRequest;
import com.learning.cart_service.core.service.catalog.CatalogService;
import com.learning.cart_service.core.service.catalog.CatalogServiceRequest;
import com.learning.cart_service.exception.BusinessRuleViolationException;

import java.time.Clock;

/**
 * Adds a product to the cart (TDD §12.5). A cart keeps one line per product:
 * <ul>
 *   <li>Same supplier as the existing line (or none requested): the quantity is added to that line.</li>
 *   <li>A different preferred supplier: the line moves to that supplier's offer with the requested quantity.</li>
 *   <li>New product: the cheapest in-stock offer is chosen, or the preferred supplier's.</li>
 * </ul>
 * In merge mode (guest cart on login) an existing line becomes max(existing, requested) and over-limit
 * quantities are capped, so replaying the same merge changes nothing.
 */
public class AddCartItemUsecase implements Usecase<AddCartItemUsecaseRequest, AddCartItemUsecaseResponse> {

    private final CartService cartService;
    private final CatalogService catalogService;
    private final Clock clock;

    public AddCartItemUsecase(CartService cartService, CatalogService catalogService, Clock clock) {
        this.cartService = cartService;
        this.catalogService = catalogService;
        this.clock = clock;
    }

    @Override
    public AddCartItemUsecaseResponse execute(AddCartItemUsecaseRequest request) {
        catalogService.execute(CatalogServiceRequest.builder()
                .operation(CatalogServiceRequest.Operation.REQUIRE_SELLABLE_PRODUCT)
                .productId(request.getProductId())
                .build());

        var items = cartService.execute(CartServiceRequest.builder()
                .operation(CartServiceRequest.Operation.FIND_ALL)
                .userId(request.getUserId())
                .build()).getItems();
        var existing = items.stream().filter(i -> i.productId().equals(request.getProductId())).findFirst();
        var otherLines = items.stream().filter(i -> !i.productId().equals(request.getProductId())).toList();
        var snapshot = catalogService.execute(CatalogServiceRequest.builder()
                .operation(CatalogServiceRequest.Operation.SNAPSHOT)
                .productId(request.getProductId())
                .build()).getSnapshot();

        boolean sameLine = existing.isPresent()
                && (request.getPreferredSupplierId() == null
                || request.getPreferredSupplierId().equals(existing.get().supplierId()));

        int quantity = request.getQuantity();
        if (sameLine) {
            quantity = request.isMerge()
                    ? Math.max(existing.get().quantity(), quantity)
                    : existing.get().quantity() + quantity;
        }
        quantity = applyQuantityLimit(quantity, request.isMerge());

        Offer offer;
        if (sameLine) {
            var existingOfferId = existing.get().offerId();
            offer = snapshot.offers().stream().filter(o -> o.offerId().equals(existingOfferId)).findFirst()
                    .orElseThrow(() -> new BusinessRuleViolationException("OFFER_UNAVAILABLE",
                            "The offer for this item is no longer available. Remove it and add it again."));
            int available = snapshot.availableFor(offer.offerId());
            if (available < quantity) {
                throw CatalogService.stockViolation(available);
            }
        } else {
            offer = catalogService.execute(CatalogServiceRequest.builder()
                    .operation(CatalogServiceRequest.Operation.CHOOSE_OFFER)
                    .snapshot(snapshot)
                    .preferredSupplierId(request.getPreferredSupplierId())
                    .quantity(quantity)
                    .build()).getOffer();
        }

        cartService.execute(CartServiceRequest.builder()
                .operation(CartServiceRequest.Operation.REQUIRE_SAME_CURRENCY)
                .items(otherLines)
                .currency(offer.currency())
                .build());
        if (existing.isEmpty()) {
            cartService.execute(CartServiceRequest.builder()
                    .operation(CartServiceRequest.Operation.REQUIRE_ROOM_FOR_NEW_LINE)
                    .items(items)
                    .build());
        }

        var now = clock.instant();
        CartItem item = sameLine
                ? existing.get().withQuantity(quantity, now).withPrice(offer.price(), now)
                : new CartItem(offer.offerId(), request.getProductId(), offer.supplierId(), quantity,
                offer.price(), offer.currency(), now, now);
        if (existing.isPresent() && !sameLine) {
            cartService.execute(CartServiceRequest.builder()
                    .operation(CartServiceRequest.Operation.REPLACE)
                    .userId(request.getUserId())
                    .itemId(existing.get().offerId())
                    .item(item)
                    .build());
        } else {
            cartService.execute(CartServiceRequest.builder()
                    .operation(CartServiceRequest.Operation.SAVE)
                    .userId(request.getUserId())
                    .item(item)
                    .build());
        }
        return new AddCartItemUsecaseResponse(item);
    }

    private int applyQuantityLimit(int quantity, boolean cap) {
        if (cap) {
            return Math.min(quantity, cartService.execute(CartServiceRequest.builder()
                    .operation(CartServiceRequest.Operation.LIMITS)
                    .build()).getMaxQuantityPerItem());
        }
        cartService.execute(CartServiceRequest.builder()
                .operation(CartServiceRequest.Operation.REQUIRE_QUANTITY_WITHIN_LIMIT)
                .quantity(quantity)
                .build());
        return quantity;
    }
}
