package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.domain.CartView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCartItemProcessorResponse implements ProcessorResponse {
    private CartView cart;
}
