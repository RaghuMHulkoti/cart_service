package com.learning.cart_service.core.usecase;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartUsecaseResponse implements UsecaseResponse {
    private List<MergeCartEntryResult> results;
}
