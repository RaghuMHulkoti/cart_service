package com.learning.cart_service.core.usecase;

import com.learning.cart_service.core.domain.CartView;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetCartUsecaseRequest implements UsecaseRequest {
    private UUID userId;
}
