package com.learning.cart_service.gateway;

import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.exception.BusinessRuleViolationException;
import com.learning.cart_service.exception.DependencyUnavailableException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Oracle-backed cart store: one {@code cart_items} row per line, keyed by (user_id, offer_id). */
public class JdbcCartGateway implements CartGateway {

    private static final String SELECT = """
            SELECT offer_id, product_id, supplier_id, quantity, unit_price, currency, added_at, updated_at
            FROM cart_items WHERE user_id = ?""";

    private static final String UPDATE = """
            UPDATE cart_items SET quantity = ?, unit_price = ?, updated_at = ?
            WHERE user_id = ? AND offer_id = ?""";

    private static final String INSERT = """
            INSERT INTO cart_items
                (user_id, offer_id, product_id, supplier_id, quantity, unit_price, currency, added_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public JdbcCartGateway(JdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    @Override
    public List<CartItem> findAll(UUID userId) {
        return guard("read cart", () -> jdbc.query(SELECT, (rs, n) -> map(rs), raw(userId)));
    }

    @Override
    public Optional<CartItem> find(UUID userId, UUID offerId) {
        return guard("read cart item", () -> jdbc.query(SELECT + " AND offer_id = ?", (rs, n) -> map(rs),
                raw(userId), raw(offerId)).stream().findFirst());
    }

    @Override
    public void save(UUID userId, CartItem item) {
        guard("write cart item", () -> {
            upsert(userId, item);
            return null;
        });
    }

    @Override
    public void replace(UUID userId, UUID oldOfferId, CartItem item) {
        guard("write cart item", () -> tx.execute(status -> {
            jdbc.update("DELETE FROM cart_items WHERE user_id = ? AND offer_id = ?", raw(userId), raw(oldOfferId));
            upsert(userId, item);
            return null;
        }));
    }

    @Override
    public void remove(UUID userId, UUID offerId) {
        guard("remove cart item", () ->
                jdbc.update("DELETE FROM cart_items WHERE user_id = ? AND offer_id = ?", raw(userId), raw(offerId)));
    }

    @Override
    public void clear(UUID userId) {
        guard("clear cart", () -> jdbc.update("DELETE FROM cart_items WHERE user_id = ?", raw(userId)));
    }

    /** Deletes lines not touched for {@code maxAge}; returns how many were removed. */
    public int purgeOlderThan(Duration maxAge, Instant now) {
        return guard("purge carts", () ->
                jdbc.update("DELETE FROM cart_items WHERE updated_at < ?", Timestamp.from(now.minus(maxAge))));
    }

    /**
     * Update, then insert if nothing matched. Two concurrent first-time adds of the same line collide on the
     * primary key and the loser surfaces as CART_CONFLICT, so no line is ever duplicated.
     */
    private void upsert(UUID userId, CartItem i) {
        int updated = jdbc.update(UPDATE, i.quantity(), i.unitPrice(), Timestamp.from(i.updatedAt()),
                raw(userId), raw(i.offerId()));
        if (updated == 0) {
            jdbc.update(INSERT, raw(userId), raw(i.offerId()), raw(i.productId()), raw(i.supplierId()),
                    i.quantity(), i.unitPrice(), i.currency(), Timestamp.from(i.addedAt()),
                    Timestamp.from(i.updatedAt()));
        }
    }

    private static CartItem map(ResultSet rs) throws SQLException {
        return new CartItem(uuid(rs.getBytes("offer_id")), uuid(rs.getBytes("product_id")),
                uuid(rs.getBytes("supplier_id")), rs.getInt("quantity"), rs.getBigDecimal("unit_price"),
                rs.getString("currency").trim(), rs.getTimestamp("added_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    static byte[] raw(UUID id) {
        return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array();
    }

    static UUID uuid(byte[] raw) {
        var buffer = ByteBuffer.wrap(raw);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    private <T> T guard(String action, Supplier<T> call) {
        try {
            return call.get();
        } catch (DuplicateKeyException e) {
            throw new BusinessRuleViolationException("CART_CONFLICT",
                    "The cart was changed by another request. Retry.");
        } catch (DataAccessException e) {
            throw new DependencyUnavailableException("Cart store unavailable: could not " + action, e);
        }
    }
}
