package com.learning.cart_service.web.response;

import com.learning.cart_service.core.domain.LineAvailability;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        List<Line> items,
        int totalQuantity,
        BigDecimal subtotal,
        String currency,
        boolean checkoutReady
) {

    /** {@code itemId} is the offer id; {@code unitPrice} is live, {@code addedPrice} is what the line was added at. */
    public record Line(
            UUID itemId,
            UUID productId,
            UUID supplierId,
            String title,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal addedPrice,
            boolean priceChanged,
            BigDecimal lineTotal,
            String currency,
            LineAvailability availability,
            int availableQuantity,
            Instant addedAt
    ) {
    }
}
