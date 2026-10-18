package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.usecase.ClearCartUsecase;

import java.util.UUID;

public class ClearCartProcessor implements Processor<ClearCartProcessor.Request, ClearCartProcessor.Response> {

    public record Request(UUID userId) implements ProcessorRequest {
    }

    public record Response() implements ProcessorResponse {
    }

    private final ClearCartUsecase clearCartUsecase;

    public ClearCartProcessor(ClearCartUsecase clearCartUsecase) {
        this.clearCartUsecase = clearCartUsecase;
    }

    @Override
    public Response process(Request request) {
        clearCartUsecase.execute(new ClearCartUsecase.Request(request.userId()));
        return new Response();
    }
}
