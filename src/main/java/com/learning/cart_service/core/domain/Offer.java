package com.learning.cart_service.core.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only view of a supplier offer, as returned by Supplier Service. {@code offerId} is the
 * supplier_product id, the key Inventory uses for stock.
 */
public record Offer(
        UUID offerId,
        UUID supplierId,
        UUID productId,
        BigDecimal price,
        String currency,
        Boolean active
) {

    public boolean isActive() {
        return !Boolean.FALSE.equals(active);
    }
}
