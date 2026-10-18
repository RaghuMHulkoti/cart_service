package com.learning.cart_service.core.usecase;

import com.learning.cart_service.core.domain.CartItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemUsecaseResponse implements UsecaseResponse {
    private CartItem item;
}
