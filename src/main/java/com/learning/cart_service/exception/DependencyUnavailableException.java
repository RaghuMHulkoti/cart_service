package com.learning.cart_service.exception;

/** The cart database or a downstream service did not answer. Writes refuse rather than silently drop (TDD §8.6). Maps to 503. */
public class DependencyUnavailableException extends RuntimeException {

    public DependencyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
