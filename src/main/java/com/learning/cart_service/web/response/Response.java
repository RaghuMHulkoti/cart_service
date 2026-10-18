package com.learning.cart_service.web.response;

import com.learning.cart_service.common.StatusCode;

public record Response<T>(StatusCode status, String message, T body) {
}
