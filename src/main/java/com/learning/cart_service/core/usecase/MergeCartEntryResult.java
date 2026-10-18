package com.learning.cart_service.core.usecase;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartEntryResult {
    private UUID productId;
    private boolean merged;
    private String errorCode;
    private String message;
}
