package com.learning.cart_service.core.usecase;

import com.learning.cart_service.exception.BusinessRuleViolationException;
import com.learning.cart_service.exception.DependencyUnavailableException;
import com.learning.cart_service.exception.ProductNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Merges a guest cart into the user's cart on login. Partial success: a line that cannot be merged is
 * reported and the rest still go through. Each line uses {@link AddCartItemUsecase} in merge mode, so the
 * whole call can be retried safely.
 */
public class MergeCartUsecase implements Usecase<MergeCartUsecase.Request, MergeCartUsecase.Response> {

    public record Entry(UUID productId, int quantity, UUID preferredSupplierId) {
    }

    public record Request(UUID userId, List<Entry> entries) implements UsecaseRequest {
    }

    /** {@code errorCode} is null when the entry was merged. */
    public record EntryResult(UUID productId, boolean merged, String errorCode, String message) {
    }

    public record Response(List<EntryResult> results) implements UsecaseResponse {
    }

    private final AddCartItemUsecase addCartItemUsecase;

    public MergeCartUsecase(AddCartItemUsecase addCartItemUsecase) {
        this.addCartItemUsecase = addCartItemUsecase;
    }

    @Override
    public Response execute(Request request) {
        List<EntryResult> results = new ArrayList<>();
        for (var entry : request.entries()) {
            try {
                addCartItemUsecase.execute(new AddCartItemUsecase.Request(request.userId(), entry.productId(),
                        entry.quantity(), entry.preferredSupplierId(), true));
                results.add(new EntryResult(entry.productId(), true, null, null));
            } catch (BusinessRuleViolationException e) {
                results.add(new EntryResult(entry.productId(), false, e.getErrorCode(), e.getMessage()));
            } catch (ProductNotFoundException e) {
                results.add(new EntryResult(entry.productId(), false, "NOT_FOUND", e.getMessage()));
            } catch (DependencyUnavailableException e) {
                results.add(new EntryResult(entry.productId(), false, "DEPENDENCY_UNAVAILABLE", e.getMessage()));
            }
        }
        return new Response(results);
    }
}
