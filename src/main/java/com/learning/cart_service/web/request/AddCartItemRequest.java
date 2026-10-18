package com.learning.cart_service.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCartItemRequest {
    private @NotNull UUID productId;
    private @Min(1) int quantity;
    private UUID preferredSupplierId;
}
