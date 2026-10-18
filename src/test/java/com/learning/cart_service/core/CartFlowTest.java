package com.learning.cart_service.core;

import com.learning.cart_service.config.CartProperties;
import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.core.domain.LineAvailability;
import com.learning.cart_service.core.domain.Offer;
import com.learning.cart_service.core.domain.ProductInfo;
import com.learning.cart_service.core.service.cart.CartService;
import com.learning.cart_service.core.service.catalog.CatalogService;
import com.learning.cart_service.core.usecase.*;
import com.learning.cart_service.core.usecase.*;
import com.learning.cart_service.core.usecase.*;
import com.learning.cart_service.core.usecase.*;
import com.learning.cart_service.core.usecase.*;
import com.learning.cart_service.exception.BusinessRuleViolationException;
import com.learning.cart_service.exception.CartItemNotFoundException;
import com.learning.cart_service.exception.DependencyUnavailableException;
import com.learning.cart_service.exception.ProductNotFoundException;
import com.learning.cart_service.gateway.CartGateway;
import com.learning.cart_service.gateway.InventoryGateway;
import com.learning.cart_service.gateway.ProductGateway;
import com.learning.cart_service.gateway.SupplierOfferGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartFlowTest {

    final UUID user = UUID.randomUUID();
    final UUID product = UUID.randomUUID();
    final UUID supplierA = UUID.randomUUID();
    final UUID supplierB = UUID.randomUUID();
    final Offer offerA = offer(supplierA, product, "100.00");
    final Offer offerB = offer(supplierB, product, "90.00");

    Map<UUID, ProductInfo> products = new HashMap<>();
    Map<UUID, List<Offer>> offers = new HashMap<>();
    Map<UUID, Integer> stock = new HashMap<>();
    boolean supplierDown;
    boolean inventoryDown;
    FakeCartGateway carts = new FakeCartGateway();

    AddCartItemUsecase add;
    UpdateCartItemUsecase update;
    RemoveCartItemUsecase remove;
    GetCartUsecase get;
    MergeCartUsecase merge;

    @BeforeEach
    void setUp() {
        products.put(product, new ProductInfo(product, "Phone", "ACTIVE"));
        offers.put(product, new ArrayList<>(List.of(offerA, offerB)));
        stock.put(offerA.offerId(), 5);
        stock.put(offerB.offerId(), 5);

        ProductGateway productGateway = new ProductGateway() {
            public Optional<ProductInfo> findById(UUID id) {
                return Optional.ofNullable(products.get(id));
            }

            public Map<UUID, ProductInfo> findByIds(Collection<UUID> ids) {
                Map<UUID, ProductInfo> result = new HashMap<>();
                ids.forEach(id -> {
                    if (products.containsKey(id)) result.put(id, products.get(id));
                });
                return result;
            }
        };
        SupplierOfferGateway offerGateway = productId -> {
            if (supplierDown) throw new DependencyUnavailableException("Supplier Service unavailable", null);
            return offers.getOrDefault(productId, List.of());
        };
        InventoryGateway inventoryGateway = (productId, offerIds) -> {
            if (inventoryDown) throw new DependencyUnavailableException("Inventory Service unavailable", null);
            Map<UUID, Integer> result = new HashMap<>();
            offerIds.forEach(id -> result.put(id, stock.getOrDefault(id, 0)));
            return result;
        };
        var catalog = new CatalogService(productGateway, offerGateway, inventoryGateway, Runnable::run);
        var cartService = new CartService(carts, new CartProperties(2, 10, Duration.ofDays(30)));
        var clock = Clock.systemUTC();
        add = new AddCartItemUsecase(cartService, catalog, clock);
        update = new UpdateCartItemUsecase(cartService, catalog, clock);
        remove = new RemoveCartItemUsecase(cartService);
        get = new GetCartUsecase(cartService, catalog);
        merge = new MergeCartUsecase(add);
    }

    @Test
    void addPicksCheapestInStockOffer() {
        var item = add(2, null).getItem();

        assertThat(item.offerId()).isEqualTo(offerB.offerId());
        assertThat(item.unitPrice()).isEqualByComparingTo("90.00");
    }

    @Test
    void addSkipsCheapestOfferWhenItCannotCoverQuantity() {
        stock.put(offerB.offerId(), 1);

        assertThat(add(2, null).getItem().offerId()).isEqualTo(offerA.offerId());
    }

    @Test
    void addHonoursPreferredSupplier() {
        assertThat(add(1, supplierA).getItem().offerId()).isEqualTo(offerA.offerId());
    }

    @Test
    void addAccumulatesQuantityOnTheSameLine() {
        add(2, null);
        add(3, null);

        assertThat(carts.items).hasSize(1);
        assertThat(carts.items.values().iterator().next().quantity()).isEqualTo(5);
    }

    @Test
    void addWithOtherSupplierMovesTheLine() {
        add(2, supplierB);
        add(1, supplierA);

        assertThat(carts.items).containsOnlyKeys(offerA.offerId());
        assertThat(carts.items.get(offerA.offerId()).quantity()).isEqualTo(1);
    }

    @Test
    void addRejectsOutOfStockAndInsufficientStock() {
        stock.put(offerA.offerId(), 0);
        stock.put(offerB.offerId(), 3);

        assertThatThrownBy(() -> add(4, null)).isInstanceOfSatisfying(BusinessRuleViolationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("INSUFFICIENT_STOCK"));

        stock.put(offerB.offerId(), 0);
        assertThatThrownBy(() -> add(1, null)).isInstanceOfSatisfying(BusinessRuleViolationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("OUT_OF_STOCK"));
    }

    @Test
    void addRejectsUnknownAndInactiveProducts() {
        assertThatThrownBy(() -> add.execute(new AddCartItemUsecaseRequest(user, UUID.randomUUID(), 1, null, false)))
                .isInstanceOf(ProductNotFoundException.class);

        products.put(product, new ProductInfo(product, "Phone", "DELISTED"));
        assertThatThrownBy(() -> add(1, null)).isInstanceOfSatisfying(BusinessRuleViolationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("PRODUCT_UNAVAILABLE"));
    }

    @Test
    void addEnforcesQuantityAndLineLimits() {
        assertThatThrownBy(() -> add(11, null)).isInstanceOfSatisfying(BusinessRuleViolationException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo("QUANTITY_LIMIT_EXCEEDED"));

        add(1, null);
        for (int i = 0; i < 2; i++) {
            var other = newProduct("5.00");
            try {
                add.execute(new AddCartItemUsecaseRequest(user, other, 1, null, false));
            } catch (BusinessRuleViolationException e) {
                assertThat(e.getErrorCode()).isEqualTo("CART_FULL");
                return;
            }
        }
        throw new AssertionError("third distinct line should have been rejected (limit is 2)");
    }

    @Test
    void addRejectsMixedCurrencies() {
        add(1, null);
        var other = newProduct("5.00");
        offers.put(other, List.of(new Offer(UUID.randomUUID(), supplierA, other, new BigDecimal("5"), "USD", true)));
        stock.put(offers.get(other).getFirst().offerId(), 5);

        assertThatThrownBy(() -> add.execute(new AddCartItemUsecaseRequest(user, other, 1, null, false)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("CURRENCY_MISMATCH"));
    }

    @Test
    void addRefusesWhenDownstreamIsDown() {
        inventoryDown = true;

        assertThatThrownBy(() -> add(1, null)).isInstanceOf(DependencyUnavailableException.class);
        assertThat(carts.items).isEmpty();
    }

    @Test
    void updateRaisesWithStockCheckAndLowersWithoutOne() {
        var itemId = add(2, null).getItem().offerId();

        update.execute(new UpdateCartItemUsecaseRequest(user, itemId, 5));
        assertThat(carts.items.get(itemId).quantity()).isEqualTo(5);

        assertThatThrownBy(() -> update.execute(new UpdateCartItemUsecaseRequest(user, itemId, 6)))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("INSUFFICIENT_STOCK"));

        stock.put(itemId, 0);
        inventoryDown = true;
        update.execute(new UpdateCartItemUsecaseRequest(user, itemId, 1));
        assertThat(carts.items.get(itemId).quantity()).isEqualTo(1);
    }

    @Test
    void updateUnknownItemIs404() {
        assertThatThrownBy(() -> update.execute(new UpdateCartItemUsecaseRequest(user, UUID.randomUUID(), 1)))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void removeIsIdempotent() {
        var itemId = add(1, null).getItem().offerId();

        remove.execute(new RemoveCartItemUsecaseRequest(user, itemId));
        remove.execute(new RemoveCartItemUsecaseRequest(user, itemId));

        assertThat(carts.items).isEmpty();
    }

    @Test
    void renderUsesLivePriceAndFlagsChange() {
        add(2, null);
        offers.put(product, List.of(offerA, offer(supplierB, product, "95.00", offerB.offerId())));

        var cart = get.execute(new GetCartUsecaseRequest(user)).getCart();

        var line = cart.lines().getFirst();
        assertThat(line.unitPrice()).isEqualByComparingTo("95.00");
        assertThat(line.priceChanged()).isTrue();
        assertThat(cart.subtotal()).isEqualByComparingTo("190.00");
        assertThat(cart.totalQuantity()).isEqualTo(2);
        assertThat(cart.checkoutReady()).isTrue();
    }

    @Test
    void renderReportsAvailabilityStates() {
        var itemId = add(3, null).getItem().offerId();

        stock.put(itemId, 2);
        assertThat(line().availability()).isEqualTo(LineAvailability.INSUFFICIENT_STOCK);
        assertThat(line().availableQuantity()).isEqualTo(2);

        stock.put(itemId, 0);
        assertThat(line().availability()).isEqualTo(LineAvailability.OUT_OF_STOCK);
        assertThat(get.execute(new GetCartUsecaseRequest(user)).getCart().checkoutReady()).isFalse();
    }

    @Test
    void renderMarksDeletedProductAndRemovedOfferUnavailableAndExcludesThemFromSubtotal() {
        add(1, null);
        offers.put(product, List.of(offerA));
        assertThat(line().availability()).isEqualTo(LineAvailability.UNAVAILABLE);
        assertThat(line().lineTotal()).isNull();

        offers.put(product, List.of(offerA, offerB));
        products.remove(product);
        var cart = get.execute(new GetCartUsecaseRequest(user)).getCart();
        assertThat(cart.lines().getFirst().availability()).isEqualTo(LineAvailability.UNAVAILABLE);
        assertThat(cart.subtotal()).isEqualByComparingTo("0");
    }

    @Test
    void renderDegradesWhenDownstreamIsDown() {
        add(1, null);

        inventoryDown = true;
        assertThat(line().availability()).isEqualTo(LineAvailability.UNKNOWN);
        assertThat(line().unitPrice()).isEqualByComparingTo("90.00");
        assertThat(get.execute(new GetCartUsecaseRequest(user)).getCart().checkoutReady()).isFalse();

        inventoryDown = false;
        supplierDown = true;
        assertThat(line().availability()).isEqualTo(LineAvailability.UNKNOWN);
        assertThat(line().title()).isEqualTo("Phone");
    }

    @Test
    void renderOfEmptyCart() {
        var cart = get.execute(new GetCartUsecaseRequest(user)).getCart();

        assertThat(cart.lines()).isEmpty();
        assertThat(cart.checkoutReady()).isFalse();
        assertThat(cart.subtotal()).isEqualByComparingTo("0");
    }

    @Test
    void mergeIsPartialAndRetrySafe() {
        add(2, null);
        var missing = UUID.randomUUID();
        var entries = List.of(
                new MergeCartEntry(product, 4, null),
                new MergeCartEntry(missing, 1, null),
                new MergeCartEntry(product, 99, null));

        var first = merge.execute(new MergeCartUsecaseRequest(user, entries)).getResults();
        var second = merge.execute(new MergeCartUsecaseRequest(user, entries)).getResults();

        assertThat(first).extracting(MergeCartEntryResult::isMerged).containsExactly(true, false, false);
        assertThat(first.get(1).getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(second).usingRecursiveComparison().isEqualTo(first);
        // first entry: max(existing 2, guest 4) = 4. third: 99 is capped to the line limit of 10, which stock (5) can't cover
        assertThat(carts.items.get(offerB.offerId()).quantity()).isEqualTo(4);
        assertThat(first.get(2).getErrorCode()).isEqualTo("INSUFFICIENT_STOCK");
    }

    private AddCartItemUsecaseResponse add(int quantity, UUID supplier) {
        return add.execute(new AddCartItemUsecaseRequest(user, product, quantity, supplier, false));
    }

    private com.learning.cart_service.core.domain.CartLine line() {
        return get.execute(new GetCartUsecaseRequest(user)).getCart().lines().getFirst();
    }

    private UUID newProduct(String price) {
        var id = UUID.randomUUID();
        products.put(id, new ProductInfo(id, "Other", "ACTIVE"));
        var o = offer(supplierA, id, price);
        offers.put(id, List.of(o));
        stock.put(o.offerId(), 5);
        return id;
    }

    private static Offer offer(UUID supplier, UUID product, String price) {
        return offer(supplier, product, price, UUID.randomUUID());
    }

    private static Offer offer(UUID supplier, UUID product, String price, UUID offerId) {
        return new Offer(offerId, supplier, product, new BigDecimal(price), "INR", true);
    }

    static class FakeCartGateway implements CartGateway {
        final Map<UUID, CartItem> items = new LinkedHashMap<>();

        public List<CartItem> findAll(UUID userId) {
            return new ArrayList<>(items.values());
        }

        public Optional<CartItem> find(UUID userId, UUID offerId) {
            return Optional.ofNullable(items.get(offerId));
        }

        public void save(UUID userId, CartItem item) {
            items.put(item.offerId(), item);
        }

        public void replace(UUID userId, UUID oldOfferId, CartItem item) {
            items.remove(oldOfferId);
            items.put(item.offerId(), item);
        }

        public void remove(UUID userId, UUID offerId) {
            items.remove(offerId);
        }

        public void clear(UUID userId) {
            items.clear();
        }
    }
}
