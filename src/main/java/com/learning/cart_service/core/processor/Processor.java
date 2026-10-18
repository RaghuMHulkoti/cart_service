package com.learning.cart_service.core.processor;

public interface Processor<T extends ProcessorRequest, R extends ProcessorResponse> {
    R process(T request);
}
