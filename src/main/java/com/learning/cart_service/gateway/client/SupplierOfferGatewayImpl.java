package com.learning.cart_service.gateway.client;

import com.learning.cart_service.core.domain.Offer;
import com.learning.cart_service.exception.DependencyUnavailableException;
import com.learning.cart_service.gateway.SupplierOfferGateway;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

/** Calls Supplier Service {@code GET /api/v1/internal/offers?productId=}, a plain JSON array of offers. */
public class SupplierOfferGatewayImpl implements SupplierOfferGateway {

    private static final ParameterizedTypeReference<List<Offer>> OFFER_LIST = new ParameterizedTypeReference<>() {};

    private final RestClient supplierRestClient;

    public SupplierOfferGatewayImpl(RestClient supplierRestClient) {
        this.supplierRestClient = supplierRestClient;
    }

    @Override
    public List<Offer> findActiveOffers(UUID productId) {
        try {
            var offers = supplierRestClient.get()
                    .uri(uri -> uri.path("/api/v1/internal/offers").queryParam("productId", productId).build())
                    .retrieve()
                    .body(OFFER_LIST);
            return offers == null ? List.of() : offers;
        } catch (RestClientException e) {
            throw new DependencyUnavailableException("Supplier Service unavailable", e);
        }
    }
}
