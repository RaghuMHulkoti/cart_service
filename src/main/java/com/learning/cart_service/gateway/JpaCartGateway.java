package com.learning.cart_service.gateway;

import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.exception.BusinessRuleViolationException;
import com.learning.cart_service.exception.DependencyUnavailableException;
import com.learning.cart_service.gateway.entity.CartItemEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Oracle-backed cart store through JPA: one {@code cart_items} row per line, keyed by (user_id, offer_id). */
public class JpaCartGateway implements CartGateway {

    private final CartItemRepository repository;
    private final TransactionTemplate tx;

    public JpaCartGateway(CartItemRepository repository, TransactionTemplate tx) {
        this.repository = repository;
        this.tx = tx;
    }

    @Override
    public List<CartItem> findAll(UUID userId) {
        return guard("read cart", () -> repository.findByUserId(userId).stream().map(JpaCartGateway::toDomain).toList());
    }

    @Override
    public Optional<CartItem> find(UUID userId, UUID offerId) {
        return guard("read cart item", () ->
                repository.findById(new CartItemEntity.Key(userId, offerId)).map(JpaCartGateway::toDomain));
    }

    @Override
    public void save(UUID userId, CartItem item) {
        guard("write cart item", () -> repository.saveAndFlush(toEntity(userId, item)));
    }

    @Override
    public void replace(UUID userId, UUID oldOfferId, CartItem item) {
        guard("write cart item", () -> tx.execute(status -> {
            repository.deleteLine(userId, oldOfferId);
            return repository.saveAndFlush(toEntity(userId, item));
        }));
    }

    @Override
    public void remove(UUID userId, UUID offerId) {
        guard("remove cart item", () -> repository.deleteLine(userId, offerId));
    }

    @Override
    public void clear(UUID userId) {
        guard("clear cart", () -> repository.deleteCart(userId));
    }

    /** Deletes lines not touched for {@code maxAge}; returns how many were removed. */
    public int purgeOlderThan(Duration maxAge, Instant now) {
        return guard("purge carts", () -> repository.deleteUpdatedBefore(now.minus(maxAge)));
    }

    private static CartItemEntity toEntity(UUID userId, CartItem i) {
        return CartItemEntity.builder().userId(userId).offerId(i.offerId()).productId(i.productId())
                .supplierId(i.supplierId()).quantity(i.quantity()).unitPrice(i.unitPrice()).currency(i.currency())
                .addedAt(i.addedAt()).updatedAt(i.updatedAt()).build();
    }

    private static CartItem toDomain(CartItemEntity e) {
        return new CartItem(e.getOfferId(), e.getProductId(), e.getSupplierId(), e.getQuantity(), e.getUnitPrice(),
                e.getCurrency().trim(), e.getAddedAt(), e.getUpdatedAt());
    }

    /**
     * Two concurrent first-time adds of the same line collide on a primary or unique key and the loser surfaces as
     * CART_CONFLICT, so no line is ever duplicated.
     */
    private <T> T guard(String action, Supplier<T> call) {
        try {
            return call.get();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessRuleViolationException("CART_CONFLICT",
                    "The cart was changed by another request. Retry.");
        } catch (DataAccessException e) {
            throw new DependencyUnavailableException("Cart store unavailable: could not " + action, e);
        }
    }
}
