package com.learning.cart_service.core.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Live offers and stock for one product. {@code offersKnown} / {@code stockKnown} are false when the
 * corresponding service did not answer, so render can degrade instead of failing.
 */
public record ProductSnapshot(List<Offer> offers, boolean offersKnown, Map<UUID, Integer> available, boolean stockKnown) {

    public static ProductSnapshot offersDown() {
        return new ProductSnapshot(List.of(), false, Map.of(), false);
    }

    public int availableFor(UUID offerId) {
        return available.getOrDefault(offerId, 0);
    }
}
