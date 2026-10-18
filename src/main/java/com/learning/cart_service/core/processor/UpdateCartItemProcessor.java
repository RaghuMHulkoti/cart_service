package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.usecase.*;

public class UpdateCartItemProcessor implements Processor<UpdateCartItemProcessorRequest, UpdateCartItemProcessorResponse> {

    private final UpdateCartItemUsecase updateCartItemUsecase;
    private final GetCartUsecase getCartUsecase;

    public UpdateCartItemProcessor(UpdateCartItemUsecase updateCartItemUsecase, GetCartUsecase getCartUsecase) {
        this.updateCartItemUsecase = updateCartItemUsecase;
        this.getCartUsecase = getCartUsecase;
    }

    @Override
    public UpdateCartItemProcessorResponse process(UpdateCartItemProcessorRequest request) {
        updateCartItemUsecase.execute(new UpdateCartItemUsecaseRequest(request.getUserId(), request.getItemId(),
                request.getQuantity()));
        return new UpdateCartItemProcessorResponse(getCartUsecase.execute(new GetCartUsecaseRequest(request.getUserId())).getCart());
    }
}
