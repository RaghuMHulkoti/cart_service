package com.learning.cart_service.core.service.cart;

import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.core.service.ServiceResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartServiceResponse implements ServiceResponse {
    private List<CartItem> items;
    private CartItem item;
    private int maxQuantityPerItem;
}
