package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.domain.CartView;
import com.learning.cart_service.core.usecase.GetCartUsecase;

import java.util.UUID;

public class GetCartProcessor implements Processor<GetCartProcessor.Request, GetCartProcessor.Response> {

    public record Request(UUID userId) implements ProcessorRequest {
    }

    public record Response(CartView cart) implements ProcessorResponse {
    }

    private final GetCartUsecase getCartUsecase;

    public GetCartProcessor(GetCartUsecase getCartUsecase) {
        this.getCartUsecase = getCartUsecase;
    }

    @Override
    public Response process(Request request) {
        return new Response(getCartUsecase.execute(new GetCartUsecase.Request(request.userId())).cart());
    }
}
