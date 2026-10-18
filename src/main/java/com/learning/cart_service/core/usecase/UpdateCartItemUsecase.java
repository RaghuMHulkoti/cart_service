package com.learning.cart_service.core.usecase;

import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.core.service.CartService;
import com.learning.cart_service.core.service.CatalogService;
import com.learning.cart_service.exception.BusinessRuleViolationException;

import java.time.Clock;
import java.util.UUID;

/**
 * Sets a line's quantity. Lowering it never touches downstream services, so a customer can always shrink a
 * line whose stock has dropped. Raising it re-checks the offer and stock and refreshes the stored price.
 */
public class UpdateCartItemUsecase implements Usecase<UpdateCartItemUsecase.Request, UpdateCartItemUsecase.Response> {

    public record Request(UUID userId, UUID itemId, int quantity) implements UsecaseRequest {
    }

    public record Response(CartItem item) implements UsecaseResponse {
    }

    private final CartService cartService;
    private final CatalogService catalogService;
    private final Clock clock;

    public UpdateCartItemUsecase(CartService cartService, CatalogService catalogService, Clock clock) {
        this.cartService = cartService;
        this.catalogService = catalogService;
        this.clock = clock;
    }

    @Override
    public Response execute(Request request) {
        var item = cartService.requireItem(request.userId(), request.itemId());
        var now = clock.instant();

        if (request.quantity() <= item.quantity()) {
            var updated = item.withQuantity(request.quantity(), now);
            cartService.save(request.userId(), updated);
            return new Response(updated);
        }

        cartService.requireQuantityWithinLimit(request.quantity());
        var snapshot = catalogService.snapshot(item.productId());
        var offer = snapshot.offers().stream().filter(o -> o.offerId().equals(item.offerId())).findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("OFFER_UNAVAILABLE",
                        "The offer for this item is no longer available. Remove it and add it again."));
        int available = snapshot.availableFor(offer.offerId());
        if (available < request.quantity()) {
            throw CatalogService.stockViolation(available);
        }
        var updated = item.withQuantity(request.quantity(), now).withPrice(offer.price(), now);
        cartService.save(request.userId(), updated);
        return new Response(updated);
    }
}
