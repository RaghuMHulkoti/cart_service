package com.learning.cart_service.core.usecase;

import com.learning.cart_service.exception.BusinessRuleViolationException;
import com.learning.cart_service.exception.DependencyUnavailableException;
import com.learning.cart_service.exception.ProductNotFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges a guest cart into the user's cart on login. Partial success: a line that cannot be merged is
 * reported and the rest still go through. Each line uses {@link AddCartItemUsecase} in merge mode, so the
 * whole call can be retried safely.
 */
public class MergeCartUsecase implements Usecase<MergeCartUsecaseRequest, MergeCartUsecaseResponse> {

    private final AddCartItemUsecase addCartItemUsecase;

    public MergeCartUsecase(AddCartItemUsecase addCartItemUsecase) {
        this.addCartItemUsecase = addCartItemUsecase;
    }

    @Override
    public MergeCartUsecaseResponse execute(MergeCartUsecaseRequest request) {
        List<MergeCartEntryResult> results = new ArrayList<>();
        for (var entry : request.getEntries()) {
            try {
                addCartItemUsecase.execute(new AddCartItemUsecaseRequest(request.getUserId(), entry.getProductId(),
                        entry.getQuantity(), entry.getPreferredSupplierId(), true));
                results.add(new MergeCartEntryResult(entry.getProductId(), true, null, null));
            } catch (BusinessRuleViolationException e) {
                results.add(new MergeCartEntryResult(entry.getProductId(), false, e.getErrorCode(), e.getMessage()));
            } catch (ProductNotFoundException e) {
                results.add(new MergeCartEntryResult(entry.getProductId(), false, "NOT_FOUND", e.getMessage()));
            } catch (DependencyUnavailableException e) {
                results.add(new MergeCartEntryResult(entry.getProductId(), false, "DEPENDENCY_UNAVAILABLE",
                        e.getMessage()));
            }
        }
        return new MergeCartUsecaseResponse(results);
    }
}
