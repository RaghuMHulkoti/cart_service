package com.learning.cart_service.core.usecase;

import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.core.domain.CartLine;
import com.learning.cart_service.core.domain.CartView;
import com.learning.cart_service.core.domain.LineAvailability;
import com.learning.cart_service.core.domain.ProductInfo;
import com.learning.cart_service.core.domain.ProductSnapshot;
import com.learning.cart_service.core.service.CartService;
import com.learning.cart_service.core.service.CatalogService;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Renders the cart with live price and availability. Read-only: stored prices are not rewritten here, so a
 * customer sees {@code priceChanged} until they next change the line. A downstream outage degrades the
 * affected lines to UNKNOWN rather than failing the whole cart.
 */
public class GetCartUsecase implements Usecase<GetCartUsecase.Request, GetCartUsecase.Response> {

    public record Request(UUID userId) implements UsecaseRequest {
    }

    public record Response(CartView cart) implements UsecaseResponse {
    }

    private final CartService cartService;
    private final CatalogService catalogService;

    public GetCartUsecase(CartService cartService, CatalogService catalogService) {
        this.cartService = cartService;
        this.catalogService = catalogService;
    }

    @Override
    public Response execute(Request request) {
        var items = cartService.items(request.userId()).stream()
                .sorted(Comparator.comparing(CartItem::addedAt).thenComparing(CartItem::offerId))
                .toList();
        if (items.isEmpty()) {
            return new Response(new CartView(request.userId(), List.of(), 0, BigDecimal.ZERO, null, false));
        }

        var productIds = items.stream().map(CartItem::productId).distinct().toList();
        var products = catalogService.productsOrNull(productIds);
        var snapshots = catalogService.snapshotsOrDegrade(productIds);

        var lines = items.stream()
                .map(item -> buildLine(item, products, snapshots.get(item.productId())))
                .toList();

        var subtotal = lines.stream().map(CartLine::lineTotal).filter(t -> t != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalQuantity = lines.stream().mapToInt(l -> l.item().quantity()).sum();
        boolean ready = lines.stream().allMatch(l -> l.availability() == LineAvailability.IN_STOCK);
        return new Response(new CartView(request.userId(), lines, totalQuantity, subtotal,
                items.getFirst().currency(), ready));
    }

    private CartLine buildLine(CartItem item, Map<UUID, ProductInfo> products, ProductSnapshot snapshot) {
        ProductInfo product = products == null ? null : products.get(item.productId());
        String title = product == null ? null : product.title();

        if (products != null && (product == null || !product.isActive())) {
            return unavailable(item, title);
        }
        if (!snapshot.offersKnown()) {
            return unknown(item, title);
        }
        var offer = snapshot.offers().stream().filter(o -> o.offerId().equals(item.offerId())).findFirst();
        if (offer.isEmpty()) {
            return unavailable(item, title);
        }

        BigDecimal price = offer.get().price();
        boolean priceChanged = price.compareTo(item.unitPrice()) != 0;
        if (!snapshot.stockKnown()) {
            return new CartLine(item, title, price, priceChanged, LineAvailability.UNKNOWN, 0, total(price, item));
        }
        int available = snapshot.availableFor(item.offerId());
        var availability = available >= item.quantity() ? LineAvailability.IN_STOCK
                : available > 0 ? LineAvailability.INSUFFICIENT_STOCK : LineAvailability.OUT_OF_STOCK;
        return new CartLine(item, title, price, priceChanged, availability, available, total(price, item));
    }

    private static CartLine unavailable(CartItem item, String title) {
        return new CartLine(item, title, item.unitPrice(), false, LineAvailability.UNAVAILABLE, 0, null);
    }

    private static CartLine unknown(CartItem item, String title) {
        return new CartLine(item, title, item.unitPrice(), false, LineAvailability.UNKNOWN, 0,
                total(item.unitPrice(), item));
    }

    private static BigDecimal total(BigDecimal price, CartItem item) {
        return price.multiply(BigDecimal.valueOf(item.quantity()));
    }
}
