package com.learning.cart_service.web;

import com.learning.cart_service.core.domain.CartLine;
import com.learning.cart_service.core.domain.CartView;
import com.learning.cart_service.core.usecase.MergeCartEntryResult;
import com.learning.cart_service.web.response.CartResponse;
import com.learning.cart_service.web.response.MergeCartResponse;

import java.util.List;

final class CartResponseMapper {

    private CartResponseMapper() {
    }

    static CartResponse toResponse(CartView view) {
        return new CartResponse(view.userId(), view.lines().stream().map(CartResponseMapper::toLine).toList(),
                view.totalQuantity(), view.subtotal(), view.currency(), view.checkoutReady());
    }

    static MergeCartResponse toResponse(CartView view, List<MergeCartEntryResult> results) {
        return new MergeCartResponse(toResponse(view), results.stream()
                .map(r -> new MergeCartResponse.Result(r.getProductId(), r.isMerged(), r.getErrorCode(), r.getMessage()))
                .toList());
    }

    private static CartResponse.Line toLine(CartLine line) {
        var item = line.item();
        return new CartResponse.Line(item.offerId(), item.productId(), item.supplierId(), line.title(),
                item.quantity(), line.unitPrice(), item.unitPrice(), line.priceChanged(), line.lineTotal(),
                item.currency(), line.availability(), line.availableQuantity(), item.addedAt());
    }
}
