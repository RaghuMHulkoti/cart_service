package com.learning.cart_service.core.usecase;

import com.learning.cart_service.core.service.CartService;

import java.util.UUID;

public class ClearCartUsecase implements Usecase<ClearCartUsecase.Request, ClearCartUsecase.Response> {

    public record Request(UUID userId) implements UsecaseRequest {
    }

    public record Response() implements UsecaseResponse {
    }

    private final CartService cartService;

    public ClearCartUsecase(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public Response execute(Request request) {
        cartService.clear(request.userId());
        return new Response();
    }
}
