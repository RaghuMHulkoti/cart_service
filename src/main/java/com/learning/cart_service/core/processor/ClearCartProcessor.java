package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.usecase.ClearCartUsecase;
import com.learning.cart_service.core.usecase.ClearCartUsecaseRequest;

public class ClearCartProcessor implements Processor<ClearCartProcessorRequest, ClearCartProcessorResponse> {

    private final ClearCartUsecase clearCartUsecase;

    public ClearCartProcessor(ClearCartUsecase clearCartUsecase) {
        this.clearCartUsecase = clearCartUsecase;
    }

    @Override
    public ClearCartProcessorResponse process(ClearCartProcessorRequest request) {
        clearCartUsecase.execute(new ClearCartUsecaseRequest(request.getUserId()));
        return new ClearCartProcessorResponse();
    }
}
