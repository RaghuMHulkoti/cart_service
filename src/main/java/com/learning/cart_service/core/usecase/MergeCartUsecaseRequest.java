package com.learning.cart_service.core.usecase;

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
public class MergeCartUsecaseRequest implements UsecaseRequest {
    private UUID userId;
    private List<MergeCartEntry> entries;
}
