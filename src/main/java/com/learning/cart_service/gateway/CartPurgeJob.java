package com.learning.cart_service.gateway;

import com.learning.cart_service.config.CartProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;

/** Oracle has no key expiry, so carts untouched for {@code cart.ttl} are deleted once a day. */
public class CartPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(CartPurgeJob.class);

    private final JpaCartGateway gateway;
    private final CartProperties properties;
    private final Clock clock;

    public CartPurgeJob(JpaCartGateway gateway, CartProperties properties, Clock clock) {
        this.gateway = gateway;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${cart.purge-cron:0 30 3 * * *}")
    public void purge() {
        try {
            int removed = gateway.purgeOlderThan(properties.ttl(), clock.instant());
            log.info("Purged {} abandoned cart line(s) older than {}", removed, properties.ttl());
        } catch (RuntimeException e) {
            log.warn("Cart purge failed: {}", e.getMessage());
        }
    }
}
