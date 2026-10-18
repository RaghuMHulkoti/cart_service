package com.learning.cart_service.web;

import com.learning.cart_service.core.processor.*;
import com.learning.cart_service.common.StatusCode;
import com.learning.cart_service.core.usecase.MergeCartEntry;
import com.learning.cart_service.web.request.AddCartItemRequest;
import com.learning.cart_service.web.request.MergeCartRequest;
import com.learning.cart_service.web.request.UpdateCartItemRequest;
import com.learning.cart_service.web.response.CartResponse;
import com.learning.cart_service.web.response.MergeCartResponse;
import com.learning.cart_service.web.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Customer cart API (TDD §11.6). The caller is identified by {@code X-User-Id}, which the API gateway sets
 * after validating the JWT; this service is never exposed without the gateway in front of it.
 */
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart")
public class CartController {

    private static final String USER_HEADER = "X-User-Id";

    private final GetCartProcessor getCartProcessor;
    private final AddCartItemProcessor addCartItemProcessor;
    private final UpdateCartItemProcessor updateCartItemProcessor;
    private final RemoveCartItemProcessor removeCartItemProcessor;
    private final ClearCartProcessor clearCartProcessor;
    private final MergeCartProcessor mergeCartProcessor;

    public CartController(GetCartProcessor getCartProcessor, AddCartItemProcessor addCartItemProcessor,
                          UpdateCartItemProcessor updateCartItemProcessor,
                          RemoveCartItemProcessor removeCartItemProcessor, ClearCartProcessor clearCartProcessor,
                          MergeCartProcessor mergeCartProcessor) {
        this.getCartProcessor = getCartProcessor;
        this.addCartItemProcessor = addCartItemProcessor;
        this.updateCartItemProcessor = updateCartItemProcessor;
        this.removeCartItemProcessor = removeCartItemProcessor;
        this.clearCartProcessor = clearCartProcessor;
        this.mergeCartProcessor = mergeCartProcessor;
    }

    @GetMapping
    @Operation(summary = "Render the cart with live pricing and availability")
    public ResponseEntity<Response<CartResponse>> getCart(
            @Parameter(hidden = true) @RequestHeader(USER_HEADER) UUID userId) {
        var result = getCartProcessor.process(new GetCartProcessorRequest(userId));
        return ok("Cart retrieved successfully", CartResponseMapper.toResponse(result.getCart()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add a product; picks the cheapest in-stock offer unless preferredSupplierId is given")
    public ResponseEntity<Response<CartResponse>> addItem(
            @Parameter(hidden = true) @RequestHeader(USER_HEADER) UUID userId,
            @Valid @RequestBody AddCartItemRequest request) {
        var result = addCartItemProcessor.process(new AddCartItemProcessorRequest(
                userId, request.getProductId(), request.getQuantity(), request.getPreferredSupplierId()));
        return ok("Item added to cart", CartResponseMapper.toResponse(result.getCart()));
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Change a line's quantity")
    public ResponseEntity<Response<CartResponse>> updateItem(
            @Parameter(hidden = true) @RequestHeader(USER_HEADER) UUID userId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        var result = updateCartItemProcessor.process(
                new UpdateCartItemProcessorRequest(userId, itemId, request.getQuantity()));
        return ok("Cart item updated", CartResponseMapper.toResponse(result.getCart()));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove a line (idempotent)")
    public ResponseEntity<Response<CartResponse>> removeItem(
            @Parameter(hidden = true) @RequestHeader(USER_HEADER) UUID userId,
            @PathVariable UUID itemId) {
        var result = removeCartItemProcessor.process(new RemoveCartItemProcessorRequest(userId, itemId));
        return ok("Cart item removed", CartResponseMapper.toResponse(result.getCart()));
    }

    @DeleteMapping
    @Operation(summary = "Clear the cart")
    public ResponseEntity<Void> clear(@Parameter(hidden = true) @RequestHeader(USER_HEADER) UUID userId) {
        clearCartProcessor.process(new ClearCartProcessorRequest(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge a guest cart on login; partial success, safe to retry")
    public ResponseEntity<Response<MergeCartResponse>> merge(
            @Parameter(hidden = true) @RequestHeader(USER_HEADER) UUID userId,
            @Valid @RequestBody MergeCartRequest request) {
        var entries = request.getItems().stream()
                .map(i -> new MergeCartEntry(i.getProductId(), i.getQuantity(), i.getPreferredSupplierId()))
                .toList();
        var result = mergeCartProcessor.process(new MergeCartProcessorRequest(userId, entries));
        return ok("Cart merged", CartResponseMapper.toResponse(result.getCart(), result.getResults()));
    }

    private static <T> ResponseEntity<Response<T>> ok(String message, T body) {
        return ResponseEntity.ok(new Response<>(StatusCode.SUCCESS, message, body));
    }
}
