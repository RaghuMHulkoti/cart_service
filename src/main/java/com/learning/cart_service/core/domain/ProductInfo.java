package com.learning.cart_service.core.domain;

import java.util.UUID;

/** The slice of a catalog product the cart needs: identity, display title and whether it can be sold. */
public record ProductInfo(UUID id, String title, String status) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
