package com.learning.cart_service.web.response;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(String errorCode, String message, int status, OffsetDateTime timestamp,
                            Map<String, String> details) {
}
