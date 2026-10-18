package com.learning.cart_service.core.usecase;

import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.core.domain.Offer;
import com.learning.cart_service.exception.BusinessRuleViolationException;
import com.learning.cart_service.core.service.CartService;
import com.learning.cart_service.core.service.CatalogService;

import java.time.Clock;
import java.util.UUID;

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
public class AddCartItemUsecase implements Usecase<AddCartItemUsecase.Request, AddCartItemUsecase.Response> {

    public record Request(UUID userId, UUID productId, int quantity, UUID preferredSupplierId, boolean merge)
            implements UsecaseRequest {
    }

    public record Response(CartItem item) implements UsecaseResponse {
    }

    private final CartService cartService;
    private final CatalogService catalogService;
    private final Clock clock;

    public AddCartItemUsecase(CartService cartService, CatalogService catalogService, Clock clock) {
        this.cartService = cartService;
        this.catalogService = catalogService;
        this.clock = clock;
    }

    @Override
    public Response execute(Request request) {
        catalogService.requireSellableProduct(request.productId());

        var items = cartService.items(request.userId());
        var existing = items.stream().filter(i -> i.productId().equals(request.productId())).findFirst();
        var otherLines = items.stream().filter(i -> !i.productId().equals(request.productId())).toList();
        var snapshot = catalogService.snapshot(request.productId());

        boolean sameLine = existing.isPresent()
                && (request.preferredSupplierId() == null
                || request.preferredSupplierId().equals(existing.get().supplierId()));

        int quantity = request.quantity();
        if (sameLine) {
            quantity = request.merge()
                    ? Math.max(existing.get().quantity(), quantity)
                    : existing.get().quantity() + quantity;
        }
        quantity = applyQuantityLimit(quantity, request.merge());

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
            offer = catalogService.chooseOffer(snapshot, request.preferredSupplierId(), quantity);
        }

        cartService.requireSameCurrency(otherLines, offer.currency());
        if (existing.isEmpty()) {
            cartService.requireRoomForNewLine(items);
        }

        var now = clock.instant();
        CartItem item = sameLine
                ? existing.get().withQuantity(quantity, now).withPrice(offer.price(), now)
                : new CartItem(offer.offerId(), request.productId(), offer.supplierId(), quantity,
                offer.price(), offer.currency(), now, now);
        if (existing.isPresent() && !sameLine) {
            cartService.replace(request.userId(), existing.get().offerId(), item);
        } else {
            cartService.save(request.userId(), item);
        }
        return new Response(item);
    }

    private int applyQuantityLimit(int quantity, boolean cap) {
        if (cap) {
            return Math.min(quantity, cartService.maxQuantityPerItem());
        }
        cartService.requireQuantityWithinLimit(quantity);
        return quantity;
    }
}
