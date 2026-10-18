package com.learning.cart_service.gateway;

import com.learning.cart_service.core.domain.CartItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Cart store. Implementations throw BusinessRuleViolationException(CART_CONFLICT) on a concurrent duplicate line and DependencyUnavailableException when the store is down. */
public interface CartGateway {

    List<CartItem> findAll(UUID userId);

    Optional<CartItem> find(UUID userId, UUID offerId);

    void save(UUID userId, CartItem item);

    /** Atomically swaps a line for another offer of the same product. */
    void replace(UUID userId, UUID oldOfferId, CartItem item);

    void remove(UUID userId, UUID offerId);

    void clear(UUID userId);
}
