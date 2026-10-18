package com.learning.cart_service.web.response;

import java.util.List;
import java.util.UUID;

public record MergeCartResponse(CartResponse cart, List<Result> results) {

    /** {@code errorCode} and {@code message} are null when the entry was merged. */
    public record Result(UUID productId, boolean merged, String errorCode, String message) {
    }
}
