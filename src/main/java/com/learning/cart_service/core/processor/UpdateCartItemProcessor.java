package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.domain.CartView;
import com.learning.cart_service.core.usecase.GetCartUsecase;
import com.learning.cart_service.core.usecase.UpdateCartItemUsecase;

import java.util.UUID;

public class UpdateCartItemProcessor implements Processor<UpdateCartItemProcessor.Request, UpdateCartItemProcessor.Response> {

    public record Request(UUID userId, UUID itemId, int quantity) implements ProcessorRequest {
    }

    public record Response(CartView cart) implements ProcessorResponse {
    }

    private final UpdateCartItemUsecase updateCartItemUsecase;
    private final GetCartUsecase getCartUsecase;

    public UpdateCartItemProcessor(UpdateCartItemUsecase updateCartItemUsecase, GetCartUsecase getCartUsecase) {
        this.updateCartItemUsecase = updateCartItemUsecase;
        this.getCartUsecase = getCartUsecase;
    }

    @Override
    public Response process(Request request) {
        updateCartItemUsecase.execute(new UpdateCartItemUsecase.Request(request.userId(), request.itemId(), request.quantity()));
        return new Response(getCartUsecase.execute(new GetCartUsecase.Request(request.userId())).cart());
    }
}
