package com.learning.cart_service.core.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One cart line, stored as one row of {@code cart_items} keyed by (user_id, offer_id). offer_id is the offerId
 * (a supplier_product id), which is also the {@code itemId} used by the API. A cart holds at most one line
 * per product. {@code unitPrice} is the price seen when the line was last written; render always compares it
 * with the live offer price.
 */
public record CartItem(
        UUID offerId,
        UUID productId,
        UUID supplierId,
        int quantity,
        BigDecimal unitPrice,
        String currency,
        Instant addedAt,
        Instant updatedAt
) {

    public CartItem withQuantity(int newQuantity, Instant now) {
        return new CartItem(offerId, productId, supplierId, newQuantity, unitPrice, currency, addedAt, now);
    }

    public CartItem withPrice(BigDecimal newPrice, Instant now) {
        return new CartItem(offerId, productId, supplierId, quantity, newPrice, currency, addedAt, now);
    }
}
