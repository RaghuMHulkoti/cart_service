package com.learning.cart_service.core.processor;

import com.learning.cart_service.core.domain.CartView;
import com.learning.cart_service.core.usecase.MergeCartEntryResult;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartProcessorResponse implements ProcessorResponse {
    private CartView cart;
    private List<MergeCartEntryResult> results;
}
