package com.learning.cart_service.core.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Rendered cart. {@code subtotal} covers only lines that are not UNAVAILABLE. {@code checkoutReady} is true
 * only when every line is IN_STOCK; UNKNOWN blocks it because stock could not be confirmed.
 */
public record CartView(
        UUID userId,
        List<CartLine> lines,
        int totalQuantity,
        BigDecimal subtotal,
        String currency,
        boolean checkoutReady
) {
}
