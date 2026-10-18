package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.domain.CartView;
import com.learning.cart_service.core.usecase.GetCartUsecase;
import com.learning.cart_service.core.usecase.MergeCartUsecase;

import java.util.List;
import java.util.UUID;

public class MergeCartProcessor implements Processor<MergeCartProcessor.Request, MergeCartProcessor.Response> {

    public record Request(UUID userId, List<MergeCartUsecase.Entry> entries) implements ProcessorRequest {
    }

    public record Response(CartView cart, List<MergeCartUsecase.EntryResult> results) implements ProcessorResponse {
    }

    private final MergeCartUsecase mergeCartUsecase;
    private final GetCartUsecase getCartUsecase;

    public MergeCartProcessor(MergeCartUsecase mergeCartUsecase, GetCartUsecase getCartUsecase) {
        this.mergeCartUsecase = mergeCartUsecase;
        this.getCartUsecase = getCartUsecase;
    }

    @Override
    public Response process(Request request) {
        var merged = mergeCartUsecase.execute(new MergeCartUsecase.Request(request.userId(), request.entries()));
        var cart = getCartUsecase.execute(new GetCartUsecase.Request(request.userId())).cart();
        return new Response(cart, merged.results());
    }
}
