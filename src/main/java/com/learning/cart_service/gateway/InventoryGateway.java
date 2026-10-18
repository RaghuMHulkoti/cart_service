package com.learning.cart_service.gateway;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface InventoryGateway {

    /** Available units per offer id, summed over warehouses. Offers with no stock are present with 0. */
    Map<UUID, Integer> availableByOffer(UUID productId, Collection<UUID> offerIds);
}
