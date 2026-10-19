package com.learning.cart_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cartServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopTrail Cart Service API")
                        .description("Pre-order assembly. Carts live in Oracle (schema CART). Price and stock are never owned here: "
                                + "they are read live from Supplier Service offers and Inventory Service. "
                                + "The caller identity arrives in the X-User-Id header set by the API gateway.")
                        .version("1.0.0"));
    }
}
