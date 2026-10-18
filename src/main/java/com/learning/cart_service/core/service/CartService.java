package com.learning.cart_service.core.service;

import com.learning.cart_service.config.CartProperties;
import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.exception.BusinessRuleViolationException;
import com.learning.cart_service.exception.CartItemNotFoundException;
import com.learning.cart_service.gateway.CartGateway;

import java.util.List;
import java.util.UUID;

/** Cart persistence plus the cart-shape rules (line limits, quantity cap, single currency). */
public class CartService {

    private final CartGateway cartGateway;
    private final CartProperties properties;

    public CartService(CartGateway cartGateway, CartProperties properties) {
        this.cartGateway = cartGateway;
        this.properties = properties;
    }

    public List<CartItem> items(UUID userId) {
        return cartGateway.findAll(userId);
    }

    public CartItem requireItem(UUID userId, UUID itemId) {
        return cartGateway.find(userId, itemId).orElseThrow(() -> new CartItemNotFoundException(itemId));
    }

    public void save(UUID userId, CartItem item) {
        cartGateway.save(userId, item);
    }

    public void replace(UUID userId, UUID oldItemId, CartItem item) {
        cartGateway.replace(userId, oldItemId, item);
    }

    public void remove(UUID userId, UUID itemId) {
        cartGateway.remove(userId, itemId);
    }

    public void clear(UUID userId) {
        cartGateway.clear(userId);
    }

    public int maxQuantityPerItem() {
        return properties.maxQuantityPerItem();
    }

    public void requireQuantityWithinLimit(int quantity) {
        if (quantity > properties.maxQuantityPerItem()) {
            throw new BusinessRuleViolationException("QUANTITY_LIMIT_EXCEEDED",
                    "At most " + properties.maxQuantityPerItem() + " units of one item per cart");
        }
    }

    public void requireRoomForNewLine(List<CartItem> items) {
        if (items.size() >= properties.maxDistinctItems()) {
            throw new BusinessRuleViolationException("CART_FULL",
                    "A cart holds at most " + properties.maxDistinctItems() + " different items");
        }
    }

    /** All lines must share one currency so the subtotal is meaningful. */
    public void requireSameCurrency(List<CartItem> otherItems, String currency) {
        boolean mixed = otherItems.stream().anyMatch(i -> !i.currency().equals(currency));
        if (mixed) {
            throw new BusinessRuleViolationException("CURRENCY_MISMATCH",
                    "All items in a cart must use the same currency");
        }
    }
}
