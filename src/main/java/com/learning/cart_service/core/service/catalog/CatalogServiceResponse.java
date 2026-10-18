package com.learning.cart_service.core.service.catalog;

import com.learning.cart_service.core.domain.Offer;
import com.learning.cart_service.core.domain.ProductInfo;
import com.learning.cart_service.core.domain.ProductSnapshot;
import com.learning.cart_service.core.service.ServiceResponse;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogServiceResponse implements ServiceResponse {
    private ProductInfo product;
    private ProductSnapshot snapshot;
    private Map<UUID, ProductSnapshot> snapshots;
    private Map<UUID, ProductInfo> products;
    private Offer offer;
}
