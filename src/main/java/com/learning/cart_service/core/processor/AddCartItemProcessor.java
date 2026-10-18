package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.usecase.*;

public class AddCartItemProcessor implements Processor<AddCartItemProcessorRequest, AddCartItemProcessorResponse> {

    private final AddCartItemUsecase addCartItemUsecase;
    private final GetCartUsecase getCartUsecase;

    public AddCartItemProcessor(AddCartItemUsecase addCartItemUsecase, GetCartUsecase getCartUsecase) {
        this.addCartItemUsecase = addCartItemUsecase;
        this.getCartUsecase = getCartUsecase;
    }

    @Override
    public AddCartItemProcessorResponse process(AddCartItemProcessorRequest request) {
        addCartItemUsecase.execute(new AddCartItemUsecaseRequest(request.getUserId(), request.getProductId(),
                request.getQuantity(), request.getPreferredSupplierId(), false));
        return new AddCartItemProcessorResponse(getCartUsecase.execute(new GetCartUsecaseRequest(request.getUserId())).getCart());
    }
}
