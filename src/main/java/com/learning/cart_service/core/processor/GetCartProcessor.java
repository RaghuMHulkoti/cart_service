package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.usecase.*;

public class GetCartProcessor implements Processor<GetCartProcessorRequest, GetCartProcessorResponse> {

    private final GetCartUsecase getCartUsecase;

    public GetCartProcessor(GetCartUsecase getCartUsecase) {
        this.getCartUsecase = getCartUsecase;

    }

    @Override
    public GetCartProcessorResponse process(GetCartProcessorRequest request) {

        return new GetCartProcessorResponse(getCartUsecase.execute(new GetCartUsecaseRequest(request.getUserId())).getCart());
    }
}
