package com.learning.cart_service.config;

import com.learning.cart_service.core.processor.AddCartItemProcessor;
import com.learning.cart_service.core.processor.ClearCartProcessor;
import com.learning.cart_service.core.processor.GetCartProcessor;
import com.learning.cart_service.core.processor.MergeCartProcessor;
import com.learning.cart_service.core.processor.RemoveCartItemProcessor;
import com.learning.cart_service.core.processor.UpdateCartItemProcessor;
import com.learning.cart_service.core.service.cart.CartService;
import com.learning.cart_service.core.service.catalog.CatalogService;
import com.learning.cart_service.core.usecase.AddCartItemUsecase;
import com.learning.cart_service.core.usecase.ClearCartUsecase;
import com.learning.cart_service.core.usecase.GetCartUsecase;
import com.learning.cart_service.core.usecase.MergeCartUsecase;
import com.learning.cart_service.core.usecase.RemoveCartItemUsecase;
import com.learning.cart_service.core.usecase.UpdateCartItemUsecase;
import com.learning.cart_service.gateway.CartGateway;
import com.learning.cart_service.gateway.CartPurgeJob;
import com.learning.cart_service.gateway.InventoryGateway;
import com.learning.cart_service.gateway.ProductGateway;
import com.learning.cart_service.gateway.CartItemRepository;
import com.learning.cart_service.gateway.JpaCartGateway;
import com.learning.cart_service.gateway.SupplierOfferGateway;
import com.learning.cart_service.gateway.client.InventoryGatewayImpl;
import com.learning.cart_service.gateway.client.ProductGatewayImpl;
import com.learning.cart_service.gateway.client.SupplierOfferGatewayImpl;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Wires the core layers by hand so that processor, use case and service classes stay
 * free of Spring stereotype annotations.
 */
@Configuration
@EnableConfigurationProperties(CartProperties.class)
@EnableScheduling
public class AppConfig {

    // =========================================================================
    // Processors
    // =========================================================================

    @Bean
    public GetCartProcessor getCartProcessor(GetCartUsecase usecase) {
        return new GetCartProcessor(usecase);
    }

    @Bean
    public AddCartItemProcessor addCartItemProcessor(AddCartItemUsecase usecase, GetCartUsecase getCartUsecase) {
        return new AddCartItemProcessor(usecase, getCartUsecase);
    }

    @Bean
    public UpdateCartItemProcessor updateCartItemProcessor(UpdateCartItemUsecase usecase, GetCartUsecase getCartUsecase) {
        return new UpdateCartItemProcessor(usecase, getCartUsecase);
    }

    @Bean
    public RemoveCartItemProcessor removeCartItemProcessor(RemoveCartItemUsecase usecase, GetCartUsecase getCartUsecase) {
        return new RemoveCartItemProcessor(usecase, getCartUsecase);
    }

    @Bean
    public ClearCartProcessor clearCartProcessor(ClearCartUsecase usecase) {
        return new ClearCartProcessor(usecase);
    }

    @Bean
    public MergeCartProcessor mergeCartProcessor(MergeCartUsecase usecase, GetCartUsecase getCartUsecase) {
        return new MergeCartProcessor(usecase, getCartUsecase);
    }

    // =========================================================================
    // Use cases
    // =========================================================================

    @Bean
    public GetCartUsecase getCartUsecase(CartService cartService, CatalogService catalogService) {
        return new GetCartUsecase(cartService, catalogService);
    }

    @Bean
    public AddCartItemUsecase addCartItemUsecase(CartService cartService, CatalogService catalogService, Clock clock) {
        return new AddCartItemUsecase(cartService, catalogService, clock);
    }

    @Bean
    public UpdateCartItemUsecase updateCartItemUsecase(CartService cartService, CatalogService catalogService, Clock clock) {
        return new UpdateCartItemUsecase(cartService, catalogService, clock);
    }

    @Bean
    public RemoveCartItemUsecase removeCartItemUsecase(CartService cartService) {
        return new RemoveCartItemUsecase(cartService);
    }

    @Bean
    public ClearCartUsecase clearCartUsecase(CartService cartService) {
        return new ClearCartUsecase(cartService);
    }

    @Bean
    public MergeCartUsecase mergeCartUsecase(AddCartItemUsecase addCartItemUsecase) {
        return new MergeCartUsecase(addCartItemUsecase);
    }

    // =========================================================================
    // Services
    // =========================================================================

    @Bean
    public CartService cartService(CartGateway cartGateway, CartProperties properties) {
        return new CartService(cartGateway, properties);
    }

    @Bean
    public CatalogService catalogService(ProductGateway productGateway, SupplierOfferGateway supplierOfferGateway,
                                         InventoryGateway inventoryGateway, Executor catalogExecutor) {
        return new CatalogService(productGateway, supplierOfferGateway, inventoryGateway, catalogExecutor);
    }

    // =========================================================================
    // Gateways
    // =========================================================================

    @Bean
    public JpaCartGateway cartGateway(CartItemRepository repository, PlatformTransactionManager transactionManager) {
        return new JpaCartGateway(repository, new TransactionTemplate(transactionManager));
    }

    @Bean
    public CartPurgeJob cartPurgeJob(JpaCartGateway gateway, CartProperties properties, Clock clock) {
        return new CartPurgeJob(gateway, properties, clock);
    }

    @Bean
    public ProductGateway productGateway(RestClient productRestClient) {
        return new ProductGatewayImpl(productRestClient);
    }

    @Bean
    public SupplierOfferGateway supplierOfferGateway(RestClient supplierRestClient) {
        return new SupplierOfferGatewayImpl(supplierRestClient);
    }

    @Bean
    public InventoryGateway inventoryGateway(RestClient inventoryRestClient) {
        return new InventoryGatewayImpl(inventoryRestClient);
    }

    // =========================================================================
    // Infrastructure
    // =========================================================================

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /** Render fans out one Supplier + Inventory lookup per product; virtual threads keep that cheap. */
    @Bean(destroyMethod = "close")
    public java.util.concurrent.ExecutorService catalogExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public RestClient productRestClient(
            ObservationRegistry observationRegistry,
            @Value("${product-service.base-url}") String baseUrl,
            @Value("${product-service.connect-timeout}") Duration connectTimeout,
            @Value("${product-service.read-timeout}") Duration readTimeout) {
        return restClient(observationRegistry, baseUrl, connectTimeout, readTimeout);
    }

    @Bean
    public RestClient supplierRestClient(
            ObservationRegistry observationRegistry,
            @Value("${supplier-service.base-url}") String baseUrl,
            @Value("${supplier-service.connect-timeout}") Duration connectTimeout,
            @Value("${supplier-service.read-timeout}") Duration readTimeout) {
        return restClient(observationRegistry, baseUrl, connectTimeout, readTimeout);
    }

    @Bean
    public RestClient inventoryRestClient(
            ObservationRegistry observationRegistry,
            @Value("${inventory-service.base-url}") String baseUrl,
            @Value("${inventory-service.connect-timeout}") Duration connectTimeout,
            @Value("${inventory-service.read-timeout}") Duration readTimeout) {
        return restClient(observationRegistry, baseUrl, connectTimeout, readTimeout);
    }

    /** Built from Boot's RestClient.Builder so every call is timed as http.client.requests. */
    private static RestClient restClient(ObservationRegistry observationRegistry, String baseUrl, Duration connectTimeout, Duration readTimeout) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().observationRegistry(observationRegistry).baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
