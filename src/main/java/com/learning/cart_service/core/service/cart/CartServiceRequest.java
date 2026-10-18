package com.learning.cart_service.core.service.cart;

import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.core.service.ServiceRequest;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartServiceRequest implements ServiceRequest {
    private Operation operation;
    private UUID userId;
    private UUID itemId;
    private CartItem item;
    private List<CartItem> items;
    private int quantity;
    private String currency;

    public enum Operation {
        FIND_ALL, REQUIRE_ITEM, SAVE, REPLACE, REMOVE, CLEAR, LIMITS, REQUIRE_QUANTITY_WITHIN_LIMIT, REQUIRE_ROOM_FOR_NEW_LINE, REQUIRE_SAME_CURRENCY
    }
}
