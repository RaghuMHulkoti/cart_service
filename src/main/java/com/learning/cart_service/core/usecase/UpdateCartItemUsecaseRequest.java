package com.learning.cart_service.core.usecase;

import com.learning.cart_service.core.domain.CartItem;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemUsecaseRequest implements UsecaseRequest {
    private UUID userId;
    private UUID itemId;
    private int quantity;
}
