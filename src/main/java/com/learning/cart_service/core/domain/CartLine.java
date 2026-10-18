package com.learning.cart_service.core.domain;

import java.math.BigDecimal;

/**
 * A stored cart line joined with live catalog data. {@code unitPrice} is the live offer price when known,
 * otherwise the price stored on the line. {@code lineTotal} is null for UNAVAILABLE lines.
 */
public record CartLine(
        CartItem item,
        String title,
        BigDecimal unitPrice,
        boolean priceChanged,
        LineAvailability availability,
        int availableQuantity,
        BigDecimal lineTotal
) {
}
