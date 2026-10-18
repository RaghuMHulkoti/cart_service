package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.domain.CartView;
import com.learning.cart_service.core.usecase.AddCartItemUsecase;
import com.learning.cart_service.core.usecase.GetCartUsecase;

import java.util.UUID;

public class AddCartItemProcessor implements Processor<AddCartItemProcessor.Request, AddCartItemProcessor.Response> {

    public record Request(UUID userId, UUID productId, int quantity, UUID preferredSupplierId)
            implements ProcessorRequest {
    }

    public record Response(CartView cart) implements ProcessorResponse {
    }

    private final AddCartItemUsecase addCartItemUsecase;
    private final GetCartUsecase getCartUsecase;

    public AddCartItemProcessor(AddCartItemUsecase addCartItemUsecase, GetCartUsecase getCartUsecase) {
        this.addCartItemUsecase = addCartItemUsecase;
        this.getCartUsecase = getCartUsecase;
    }

    @Override
    public Response process(Request request) {
        addCartItemUsecase.execute(new AddCartItemUsecase.Request(request.userId(), request.productId(),
                request.quantity(), request.preferredSupplierId(), false));
        return new Response(getCartUsecase.execute(new GetCartUsecase.Request(request.userId())).cart());
    }
}
