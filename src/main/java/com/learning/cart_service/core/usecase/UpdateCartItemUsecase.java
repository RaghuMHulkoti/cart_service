package com.learning.cart_service.core.usecase;

import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.core.service.cart.CartService;
import com.learning.cart_service.core.service.cart.CartServiceRequest;
import com.learning.cart_service.core.service.catalog.CatalogService;
import com.learning.cart_service.core.service.catalog.CatalogServiceRequest;
import com.learning.cart_service.exception.BusinessRuleViolationException;

import java.time.Clock;

/**
 * Sets a line's quantity. Lowering it never touches downstream services, so a customer can always shrink a
 * line whose stock has dropped. Raising it re-checks the offer and stock and refreshes the stored price.
 */
public class UpdateCartItemUsecase implements Usecase<UpdateCartItemUsecaseRequest, UpdateCartItemUsecaseResponse> {

    private final CartService cartService;
    private final CatalogService catalogService;
    private final Clock clock;

    public UpdateCartItemUsecase(CartService cartService, CatalogService catalogService, Clock clock) {
        this.cartService = cartService;
        this.catalogService = catalogService;
        this.clock = clock;
    }

    @Override
    public UpdateCartItemUsecaseResponse execute(UpdateCartItemUsecaseRequest request) {
        var item = cartService.execute(CartServiceRequest.builder()
                .operation(CartServiceRequest.Operation.REQUIRE_ITEM)
                .userId(request.getUserId())
                .itemId(request.getItemId())
                .build()).getItem();
        var now = clock.instant();

        if (request.getQuantity() <= item.quantity()) {
            var updated = item.withQuantity(request.getQuantity(), now);
            save(request, updated);
            return new UpdateCartItemUsecaseResponse(updated);
        }

        cartService.execute(CartServiceRequest.builder()
                .operation(CartServiceRequest.Operation.REQUIRE_QUANTITY_WITHIN_LIMIT)
                .quantity(request.getQuantity())
                .build());
        var snapshot = catalogService.execute(CatalogServiceRequest.builder()
                .operation(CatalogServiceRequest.Operation.SNAPSHOT)
                .productId(item.productId())
                .build()).getSnapshot();
        var offer = snapshot.offers().stream().filter(o -> o.offerId().equals(item.offerId())).findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("OFFER_UNAVAILABLE",
                        "The offer for this item is no longer available. Remove it and add it again."));
        int available = snapshot.availableFor(offer.offerId());
        if (available < request.getQuantity()) {
            throw CatalogService.stockViolation(available);
        }
        var updated = item.withQuantity(request.getQuantity(), now).withPrice(offer.price(), now);
        save(request, updated);
        return new UpdateCartItemUsecaseResponse(updated);
    }

    private void save(UpdateCartItemUsecaseRequest request, CartItem item) {
        cartService.execute(CartServiceRequest.builder()
                .operation(CartServiceRequest.Operation.SAVE)
                .userId(request.getUserId())
                .item(item)
                .build());
    }
}
