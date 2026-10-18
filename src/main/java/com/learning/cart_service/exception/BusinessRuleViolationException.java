package com.learning.cart_service.exception;

/** A well-formed request that breaks a cart rule (stock, limits, unavailable offer). Maps to 422. */
public class BusinessRuleViolationException extends RuntimeException {

    private final String errorCode;

    public BusinessRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
