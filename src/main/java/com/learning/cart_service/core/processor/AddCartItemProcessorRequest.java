package com.learning.cart_service.core.processor;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCartItemProcessorRequest implements ProcessorRequest {
    private UUID userId;
    private UUID productId;
    private int quantity;
    private UUID preferredSupplierId;
}
