package com.learning.cart_service.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MergeCartRequest(@NotEmpty @Size(max = 50) List<@Valid AddCartItemRequest> items) {
}
