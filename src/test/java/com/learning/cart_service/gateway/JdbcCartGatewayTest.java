package com.learning.cart_service.gateway;

import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the real migration and gateway SQL against H2 in Oracle mode. It catches schema and MERGE mistakes
 * but is not a substitute for a run against Oracle itself.
 */
class JdbcCartGatewayTest {

    final UUID user = UUID.randomUUID();
    final UUID product = UUID.randomUUID();
    final UUID supplier = UUID.randomUUID();
    JdbcCartGateway gateway;

    @BeforeEach
    void setUp() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=Oracle;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__create_cart_schema.sql")).execute(ds);
        gateway = new JdbcCartGateway(new JdbcTemplate(ds), new TransactionTemplate(new DataSourceTransactionManager(ds)));
    }

    @Test
    void saveThenReadBack() {
        var item = item(UUID.randomUUID(), product, 2, "90.50");

        gateway.save(user, item);

        assertThat(gateway.find(user, item.offerId())).contains(item);
        assertThat(gateway.findAll(user)).containsExactly(item);
        assertThat(gateway.findAll(UUID.randomUUID())).isEmpty();
    }

    @Test
    void saveUpdatesExistingLineInsteadOfDuplicating() {
        var item = item(UUID.randomUUID(), product, 2, "90.50");
        gateway.save(user, item);
        var later = item.updatedAt().plusSeconds(1);

        gateway.save(user, item.withQuantity(5, later).withPrice(new BigDecimal("80"), later));

        var all = gateway.findAll(user);
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().quantity()).isEqualTo(5);
        assertThat(all.getFirst().unitPrice()).isEqualByComparingTo("80");
        assertThat(all.getFirst().addedAt()).isEqualTo(item.addedAt());
    }

    @Test
    void secondLineForSameProductIsRejected() {
        gateway.save(user, item(UUID.randomUUID(), product, 1, "10"));

        assertThatThrownBy(() -> gateway.save(user, item(UUID.randomUUID(), product, 1, "10")))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("CART_CONFLICT"));
    }

    @Test
    void replaceSwapsOfferOfSameProduct() {
        var old = item(UUID.randomUUID(), product, 1, "10");
        var replacement = item(UUID.randomUUID(), product, 3, "9");
        gateway.save(user, old);

        gateway.replace(user, old.offerId(), replacement);

        assertThat(gateway.findAll(user)).containsExactly(replacement);
    }

    @Test
    void removeAndClear() {
        var a = item(UUID.randomUUID(), product, 1, "10");
        var b = item(UUID.randomUUID(), UUID.randomUUID(), 1, "10");
        gateway.save(user, a);
        gateway.save(user, b);

        gateway.remove(user, a.offerId());
        assertThat(gateway.findAll(user)).containsExactly(b);

        gateway.clear(user);
        assertThat(gateway.findAll(user)).isEmpty();
    }

    @Test
    void cartsAreIsolatedPerUser() {
        var other = UUID.randomUUID();
        gateway.save(user, item(UUID.randomUUID(), product, 1, "10"));
        gateway.save(other, item(UUID.randomUUID(), product, 1, "10"));

        gateway.clear(user);

        assertThat(gateway.findAll(other)).hasSize(1);
    }

    @Test
    void purgeRemovesOnlyStaleLines() {
        var now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        var old = now.minus(40, ChronoUnit.DAYS);
        var stale = new CartItem(UUID.randomUUID(), UUID.randomUUID(), supplier, 1, BigDecimal.TEN, "INR", old, old);
        var fresh = item(UUID.randomUUID(), product, 1, "10");
        gateway.save(user, stale);
        gateway.save(user, fresh);

        int removed = gateway.purgeOlderThan(Duration.ofDays(30), now);

        assertThat(removed).isEqualTo(1);
        assertThat(gateway.findAll(user)).containsExactly(fresh);
    }

    private CartItem item(UUID offerId, UUID productId, int quantity, String price) {
        var now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return new CartItem(offerId, productId, supplier, quantity, new BigDecimal(price), "INR", now, now);
    }
}
