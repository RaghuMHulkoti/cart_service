package com.learning.cart_service.gateway;

import com.learning.cart_service.core.domain.ProductInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProductGateway {

    Optional<ProductInfo> findById(UUID productId);

    /** Products that no longer exist are simply absent from the result. */
    Map<UUID, ProductInfo> findByIds(Collection<UUID> productIds);
}
