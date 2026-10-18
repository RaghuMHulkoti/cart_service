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
public class UpdateCartItemProcessorRequest implements ProcessorRequest {
    private UUID userId;
    private UUID itemId;
    private int quantity;
}
