package com.learning.cart_service.gateway.client;

import com.learning.cart_service.exception.DependencyUnavailableException;
import com.learning.cart_service.gateway.InventoryGateway;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calls Inventory Service {@code GET /api/v1/inventory/availability}. The cart only uses this as a hint:
 * the authoritative check is the reservation at checkout (TDD §16.4).
 */
public class InventoryGatewayImpl implements InventoryGateway {

    private final RestClient inventoryRestClient;

    public InventoryGatewayImpl(RestClient inventoryRestClient) {
        this.inventoryRestClient = inventoryRestClient;
    }

    @Override
    public Map<UUID, Integer> availableByOffer(UUID productId, Collection<UUID> offerIds) {
        Map<UUID, Integer> available = new HashMap<>();
        offerIds.forEach(id -> available.put(id, 0));
        if (offerIds.isEmpty()) {
            return available;
        }
        try {
            var envelope = inventoryRestClient.get()
                    .uri(uri -> uri.path("/api/v1/inventory/availability")
                            .queryParam("productId", productId)
                            .queryParam("supplierProductIds", List.copyOf(offerIds))
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Envelope>() {});
            if (envelope != null && envelope.body() != null && envelope.body().breakdown() != null) {
                envelope.body().breakdown().forEach((offerId, perWarehouse) -> {
                    if (available.containsKey(offerId) && perWarehouse != null) {
                        available.put(offerId, perWarehouse.values().stream().mapToInt(Integer::intValue).sum());
                    }
                });
            }
            return available;
        } catch (RestClientException e) {
            throw new DependencyUnavailableException("Inventory Service unavailable", e);
        }
    }

    record Envelope(Availability body) {}

    record Availability(Map<UUID, Map<UUID, Integer>> breakdown) {}
}
