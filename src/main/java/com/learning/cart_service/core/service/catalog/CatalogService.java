package com.learning.cart_service.core.service.catalog;

import com.learning.cart_service.core.domain.Offer;
import com.learning.cart_service.core.service.Service;
import com.learning.cart_service.core.domain.ProductInfo;
import com.learning.cart_service.core.domain.ProductSnapshot;
import com.learning.cart_service.exception.BusinessRuleViolationException;
import com.learning.cart_service.exception.DependencyUnavailableException;
import com.learning.cart_service.exception.ProductNotFoundException;
import com.learning.cart_service.gateway.InventoryGateway;
import com.learning.cart_service.gateway.ProductGateway;
import com.learning.cart_service.gateway.SupplierOfferGateway;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Read-side lookups against Product, Supplier and Inventory plus the offer-selection rule. The cart owns
 * none of this data, so nothing here is persisted.
 */
public class CatalogService implements Service<CatalogServiceRequest, CatalogServiceResponse> {

    private final ProductGateway productGateway;
    private final SupplierOfferGateway supplierOfferGateway;
    private final InventoryGateway inventoryGateway;
    private final Executor executor;

    public CatalogService(ProductGateway productGateway, SupplierOfferGateway supplierOfferGateway,
                          InventoryGateway inventoryGateway, Executor executor) {
        this.productGateway = productGateway;
        this.supplierOfferGateway = supplierOfferGateway;
        this.inventoryGateway = inventoryGateway;
        this.executor = executor;
    }

    @Override
    public CatalogServiceResponse execute(CatalogServiceRequest request) {
        var response = CatalogServiceResponse.builder();
        switch (request.getOperation()) {
            case REQUIRE_SELLABLE_PRODUCT -> response.product(requireSellableProduct(request.getProductId()));
            case SNAPSHOT -> response.snapshot(snapshot(request.getProductId()));
            case SNAPSHOT_OR_DEGRADE -> response.snapshot(snapshotOrDegrade(request.getProductId()));
            case SNAPSHOTS_OR_DEGRADE -> response.snapshots(snapshotsOrDegrade(request.getProductIds()));
            case PRODUCTS_OR_NULL -> response.products(productsOrNull(request.getProductIds()));
            case CHOOSE_OFFER -> response.offer(chooseOffer(request.getSnapshot(),
                    request.getPreferredSupplierId(), request.getQuantity()));
        }
        return response.build();
    }

    private ProductInfo requireSellableProduct(UUID productId) {
        var product = productGateway.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
        if (!product.isActive()) {
            throw new BusinessRuleViolationException("PRODUCT_UNAVAILABLE",
                    "Product is not available for sale: " + productId);
        }
        return product;
    }

    /** Strict lookup for writes: any downstream failure propagates as 503. */
    private ProductSnapshot snapshot(UUID productId) {
        var offers = supplierOfferGateway.findActiveOffers(productId).stream().filter(Offer::isActive).toList();
        var available = inventoryGateway.availableByOffer(productId, offers.stream().map(Offer::offerId).toList());
        return new ProductSnapshot(offers, true, available, true);
    }

    /** Lenient lookup for render: a failing service marks its part unknown instead of throwing. */
    private ProductSnapshot snapshotOrDegrade(UUID productId) {
        List<Offer> offers;
        try {
            offers = supplierOfferGateway.findActiveOffers(productId).stream().filter(Offer::isActive).toList();
        } catch (DependencyUnavailableException e) {
            return ProductSnapshot.offersDown();
        }
        try {
            var available = inventoryGateway.availableByOffer(productId, offers.stream().map(Offer::offerId).toList());
            return new ProductSnapshot(offers, true, available, true);
        } catch (DependencyUnavailableException e) {
            return new ProductSnapshot(offers, true, Map.of(), false);
        }
    }

    /** Snapshots for several products fetched in parallel. */
    private Map<UUID, ProductSnapshot> snapshotsOrDegrade(Collection<UUID> productIds) {
        Map<UUID, CompletableFuture<ProductSnapshot>> futures = new HashMap<>();
        productIds.forEach(id -> futures.put(id, CompletableFuture.supplyAsync(() -> snapshotOrDegrade(id), executor)));
        Map<UUID, ProductSnapshot> result = new HashMap<>();
        futures.forEach((id, future) -> result.put(id, future.join()));
        return result;
    }

    /**
     * Product lookup for render. Returns null when Product Service did not answer, which must not be
     * confused with a product that is missing from the result because it was deleted.
     */
    private Map<UUID, ProductInfo> productsOrNull(Collection<UUID> productIds) {
        try {
            return productGateway.findByIds(productIds);
        } catch (DependencyUnavailableException e) {
            return null;
        }
    }

    /**
     * Cheapest active offer with at least {@code quantity} in stock, optionally restricted to one supplier.
     */
    private Offer chooseOffer(ProductSnapshot snapshot, UUID preferredSupplierId, int quantity) {
        var candidates = snapshot.offers().stream()
                .filter(o -> preferredSupplierId == null || preferredSupplierId.equals(o.supplierId()))
                .sorted(Comparator.comparing(Offer::price))
                .toList();
        if (candidates.isEmpty()) {
            throw preferredSupplierId == null
                    ? new BusinessRuleViolationException("NO_OFFERS", "No active offers for this product")
                    : new BusinessRuleViolationException("PREFERRED_SUPPLIER_UNAVAILABLE",
                    "The preferred supplier has no active offer for this product");
        }
        return candidates.stream()
                .filter(o -> snapshot.availableFor(o.offerId()) >= quantity)
                .findFirst()
                .orElseThrow(() -> stockViolation(candidates.stream()
                        .mapToInt(o -> snapshot.availableFor(o.offerId())).max().orElse(0)));
    }

    public static BusinessRuleViolationException stockViolation(int available) {
        return available <= 0
                ? new BusinessRuleViolationException("OUT_OF_STOCK", "Item is out of stock")
                : new BusinessRuleViolationException("INSUFFICIENT_STOCK", "Only " + available + " unit(s) available");
    }
}
