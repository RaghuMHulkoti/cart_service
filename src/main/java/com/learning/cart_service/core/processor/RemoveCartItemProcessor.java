package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.domain.CartView;
import com.learning.cart_service.core.usecase.GetCartUsecase;
import com.learning.cart_service.core.usecase.RemoveCartItemUsecase;

import java.util.UUID;

public class RemoveCartItemProcessor implements Processor<RemoveCartItemProcessor.Request, RemoveCartItemProcessor.Response> {

    public record Request(UUID userId, UUID itemId) implements ProcessorRequest {
    }

    public record Response(CartView cart) implements ProcessorResponse {
    }

    private final RemoveCartItemUsecase removeCartItemUsecase;
    private final GetCartUsecase getCartUsecase;

    public RemoveCartItemProcessor(RemoveCartItemUsecase removeCartItemUsecase, GetCartUsecase getCartUsecase) {
        this.removeCartItemUsecase = removeCartItemUsecase;
        this.getCartUsecase = getCartUsecase;
    }

    @Override
    public Response process(Request request) {
        removeCartItemUsecase.execute(new RemoveCartItemUsecase.Request(request.userId(), request.itemId()));
        return new Response(getCartUsecase.execute(new GetCartUsecase.Request(request.userId())).cart());
    }
}
