package com.learning.cart_service.web.response;

import com.learning.cart_service.core.domain.LineAvailability;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private UUID cartId;
    private List<Line> items;
    private int totalQuantity;
    private BigDecimal subtotal;
    private String currency;
    private boolean checkoutReady;

    /** {@code itemId} is the offer id; {@code unitPrice} is live, {@code addedPrice} is what the line was added at. */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private UUID itemId;
        private UUID productId;
        private UUID supplierId;
        private String title;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal addedPrice;
        private boolean priceChanged;
        private BigDecimal lineTotal;
        private String currency;
        private LineAvailability availability;
        private int availableQuantity;
        private Instant addedAt;
    }
}
