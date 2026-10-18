package com.learning.cart_service.core.usecase;

import com.learning.cart_service.core.service.cart.CartService;
import com.learning.cart_service.core.service.cart.CartServiceRequest;

/** Idempotent: removing a line that is already gone succeeds. */
public class RemoveCartItemUsecase implements Usecase<RemoveCartItemUsecaseRequest, RemoveCartItemUsecaseResponse> {

    private final CartService cartService;

    public RemoveCartItemUsecase(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public RemoveCartItemUsecaseResponse execute(RemoveCartItemUsecaseRequest request) {
        cartService.execute(CartServiceRequest.builder()
                .operation(CartServiceRequest.Operation.REMOVE)
                .userId(request.getUserId())
                .itemId(request.getItemId())
                .build());
        return new RemoveCartItemUsecaseResponse();
    }
}
