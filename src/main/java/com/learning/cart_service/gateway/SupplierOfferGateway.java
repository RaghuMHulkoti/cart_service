package com.learning.cart_service.gateway;

import com.learning.cart_service.core.domain.Offer;

import java.util.List;
import java.util.UUID;

public interface SupplierOfferGateway {

    List<Offer> findActiveOffers(UUID productId);
}
