package com.learning.cart_service.core.domain;

public enum LineAvailability {
    /** Enough stock for the requested quantity. */
    IN_STOCK,
    /** Some stock, but less than the requested quantity. */
    INSUFFICIENT_STOCK,
    OUT_OF_STOCK,
    /** Product delisted or deleted, or the offer is gone or deactivated. */
    UNAVAILABLE,
    /** A downstream service did not answer, so the line could not be checked. */
    UNKNOWN
}
