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
public class ClearCartProcessorRequest implements ProcessorRequest {
    private UUID userId;
}
