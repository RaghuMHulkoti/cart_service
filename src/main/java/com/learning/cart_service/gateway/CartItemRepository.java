package com.learning.cart_service.gateway;

import com.learning.cart_service.gateway.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, CartItemEntity.Key> {

    List<CartItemEntity> findByUserId(UUID userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM CartItemEntity c WHERE c.userId = :userId AND c.offerId = :offerId")
    int deleteLine(@Param("userId") UUID userId, @Param("offerId") UUID offerId);

    @Transactional
    @Modifying
    @Query("DELETE FROM CartItemEntity c WHERE c.userId = :userId")
    int deleteCart(@Param("userId") UUID userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM CartItemEntity c WHERE c.updatedAt < :cutoff")
    int deleteUpdatedBefore(@Param("cutoff") Instant cutoff);
}
