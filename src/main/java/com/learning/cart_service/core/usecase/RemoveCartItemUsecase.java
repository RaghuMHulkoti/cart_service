package com.learning.cart_service.core.usecase;

import com.learning.cart_service.core.service.CartService;

import java.util.UUID;

/** Idempotent: removing a line that is already gone succeeds. */
public class RemoveCartItemUsecase implements Usecase<RemoveCartItemUsecase.Request, RemoveCartItemUsecase.Response> {

    public record Request(UUID userId, UUID itemId) implements UsecaseRequest {
    }

    public record Response() implements UsecaseResponse {
    }

    private final CartService cartService;

    public RemoveCartItemUsecase(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public Response execute(Request request) {
        cartService.remove(request.userId(), request.itemId());
        return new Response();
    }
}
