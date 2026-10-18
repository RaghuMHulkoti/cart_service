package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.usecase.*;

public class MergeCartProcessor implements Processor<MergeCartProcessorRequest, MergeCartProcessorResponse> {

    private final MergeCartUsecase mergeCartUsecase;
    private final GetCartUsecase getCartUsecase;

    public MergeCartProcessor(MergeCartUsecase mergeCartUsecase, GetCartUsecase getCartUsecase) {
        this.mergeCartUsecase = mergeCartUsecase;
        this.getCartUsecase = getCartUsecase;
    }

    @Override
    public MergeCartProcessorResponse process(MergeCartProcessorRequest request) {
        var merged = mergeCartUsecase.execute(new MergeCartUsecaseRequest(request.getUserId(), request.getEntries()));
        return new MergeCartProcessorResponse(getCartUsecase.execute(new GetCartUsecaseRequest(request.getUserId())).getCart(), merged.getResults());
    }
}
