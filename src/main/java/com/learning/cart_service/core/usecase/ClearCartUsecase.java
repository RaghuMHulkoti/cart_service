package com.learning.cart_service.core.usecase;

import com.learning.cart_service.core.service.cart.CartService;
import com.learning.cart_service.core.service.cart.CartServiceRequest;

public class ClearCartUsecase implements Usecase<ClearCartUsecaseRequest, ClearCartUsecaseResponse> {

    private final CartService cartService;

    public ClearCartUsecase(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public ClearCartUsecaseResponse execute(ClearCartUsecaseRequest request) {
        cartService.execute(CartServiceRequest.builder()
                .operation(CartServiceRequest.Operation.CLEAR)
                .userId(request.getUserId())
                .build());
        return new ClearCartUsecaseResponse();
    }
}
