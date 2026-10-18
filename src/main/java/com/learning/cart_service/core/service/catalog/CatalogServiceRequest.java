package com.learning.cart_service.core.service.catalog;

import com.learning.cart_service.core.domain.ProductSnapshot;
import com.learning.cart_service.core.service.ServiceRequest;
import java.util.Collection;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogServiceRequest implements ServiceRequest {
    private Operation operation;
    private UUID productId;
    private Collection<UUID> productIds;
    private ProductSnapshot snapshot;
    private UUID preferredSupplierId;
    private int quantity;

    public enum Operation {
        REQUIRE_SELLABLE_PRODUCT, SNAPSHOT, SNAPSHOT_OR_DEGRADE, SNAPSHOTS_OR_DEGRADE, PRODUCTS_OR_NULL, CHOOSE_OFFER
    }
}
