package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.usecase.*;

public class RemoveCartItemProcessor implements Processor<RemoveCartItemProcessorRequest, RemoveCartItemProcessorResponse> {

    private final RemoveCartItemUsecase removeCartItemUsecase;
    private final GetCartUsecase getCartUsecase;

    public RemoveCartItemProcessor(RemoveCartItemUsecase removeCartItemUsecase, GetCartUsecase getCartUsecase) {
        this.removeCartItemUsecase = removeCartItemUsecase;
        this.getCartUsecase = getCartUsecase;
    }

    @Override
    public RemoveCartItemProcessorResponse process(RemoveCartItemProcessorRequest request) {
        removeCartItemUsecase.execute(new RemoveCartItemUsecaseRequest(request.getUserId(), request.getItemId()));
        return new RemoveCartItemProcessorResponse(getCartUsecase.execute(new GetCartUsecaseRequest(request.getUserId())).getCart());
    }
}
