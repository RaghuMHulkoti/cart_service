package com.learning.cart_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Cart limits. {@code ttl} is refreshed on every write, so an abandoned cart expires after that long.
 */
@ConfigurationProperties(prefix = "cart")
public record CartProperties(int maxDistinctItems, int maxQuantityPerItem, Duration ttl) {
}
