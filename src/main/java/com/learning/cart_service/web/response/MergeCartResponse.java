package com.learning.cart_service.web.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartResponse {
    private CartResponse cart;
    private List<Result> results;

    /** {@code errorCode} and {@code message} are null when the entry was merged. */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        private UUID productId;
        private boolean merged;
        private String errorCode;
        private String message;
    }
}
