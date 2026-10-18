package com.learning.cart_service.gateway.client;

import com.learning.cart_service.core.domain.ProductInfo;
import com.learning.cart_service.exception.DependencyUnavailableException;
import com.learning.cart_service.gateway.ProductGateway;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calls Product Service's internal API ({@code /api/v1/internal/products}), which sees every non-deleted
 * product and never calls Supplier Service.
 */
public class ProductGatewayImpl implements ProductGateway {

    private static final int BATCH_SIZE = 100;

    private final RestClient productRestClient;

    public ProductGatewayImpl(RestClient productRestClient) {
        this.productRestClient = productRestClient;
    }

    @Override
    public Optional<ProductInfo> findById(UUID productId) {
        try {
            var envelope = productRestClient.get()
                    .uri("/api/v1/internal/products/{id}", productId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Envelope<Detail>>() {});
            if (envelope == null || envelope.body() == null || envelope.body().product() == null) {
                return Optional.empty();
            }
            return Optional.of(envelope.body().product().toInfo());
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new DependencyUnavailableException("Product Service unavailable", e);
        }
    }

    @Override
    public Map<UUID, ProductInfo> findByIds(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        try {
            var envelope = productRestClient.get()
                    .uri(uri -> uri.path("/api/v1/internal/products")
                            .queryParam("ids", List.copyOf(productIds))
                            .queryParam("size", BATCH_SIZE)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Envelope<Page>>() {});
            if (envelope == null || envelope.body() == null || envelope.body().items() == null) {
                return Map.of();
            }
            return envelope.body().items().stream()
                    .map(Product::toInfo)
                    .collect(Collectors.toMap(ProductInfo::id, Function.identity(), (a, b) -> a));
        } catch (RestClientException e) {
            throw new DependencyUnavailableException("Product Service unavailable", e);
        }
    }

    record Envelope<T>(T body) {}

    record Detail(Product product) {}

    record Page(List<Product> items) {}

    record Product(UUID id, String title, String status) {
        ProductInfo toInfo() {
            return new ProductInfo(id, title, status);
        }
    }
}
