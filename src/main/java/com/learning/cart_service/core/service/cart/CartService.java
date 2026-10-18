package com.learning.cart_service.core.service.cart;

import com.learning.cart_service.config.CartProperties;
import com.learning.cart_service.core.domain.CartItem;
import com.learning.cart_service.core.service.Service;
import com.learning.cart_service.exception.BusinessRuleViolationException;
import com.learning.cart_service.exception.CartItemNotFoundException;
import com.learning.cart_service.gateway.CartGateway;

import java.util.List;

/** Cart persistence plus the cart-shape rules (line limits, quantity cap, single currency). */
public class CartService implements Service<CartServiceRequest, CartServiceResponse> {

    private final CartGateway cartGateway;
    private final CartProperties properties;

    public CartService(CartGateway cartGateway, CartProperties properties) {
        this.cartGateway = cartGateway;
        this.properties = properties;
    }

    @Override
    public CartServiceResponse execute(CartServiceRequest request) {
        return switch (request.getOperation()) {
            case FIND_ALL -> CartServiceResponse.builder()
                    .items(cartGateway.findAll(request.getUserId()))
                    .build();
            case REQUIRE_ITEM -> CartServiceResponse.builder()
                    .item(cartGateway.find(request.getUserId(), request.getItemId())
                            .orElseThrow(() -> new CartItemNotFoundException(request.getItemId())))
                    .build();
            case SAVE -> {
                cartGateway.save(request.getUserId(), request.getItem());
                yield new CartServiceResponse();
            }
            case REPLACE -> {
                cartGateway.replace(request.getUserId(), request.getItemId(), request.getItem());
                yield new CartServiceResponse();
            }
            case REMOVE -> {
                cartGateway.remove(request.getUserId(), request.getItemId());
                yield new CartServiceResponse();
            }
            case CLEAR -> {
                cartGateway.clear(request.getUserId());
                yield new CartServiceResponse();
            }
            case LIMITS -> CartServiceResponse.builder()
                    .maxQuantityPerItem(properties.maxQuantityPerItem())
                    .build();
            case REQUIRE_QUANTITY_WITHIN_LIMIT -> {
                requireQuantityWithinLimit(request.getQuantity());
                yield new CartServiceResponse();
            }
            case REQUIRE_ROOM_FOR_NEW_LINE -> {
                requireRoomForNewLine(request.getItems());
                yield new CartServiceResponse();
            }
            case REQUIRE_SAME_CURRENCY -> {
                requireSameCurrency(request.getItems(), request.getCurrency());
                yield new CartServiceResponse();
            }
        };
    }

    private void requireQuantityWithinLimit(int quantity) {
        if (quantity > properties.maxQuantityPerItem()) {
            throw new BusinessRuleViolationException("QUANTITY_LIMIT_EXCEEDED",
                    "At most " + properties.maxQuantityPerItem() + " units of one item per cart");
        }
    }

    private void requireRoomForNewLine(List<CartItem> items) {
        if (items.size() >= properties.maxDistinctItems()) {
            throw new BusinessRuleViolationException("CART_FULL",
                    "A cart holds at most " + properties.maxDistinctItems() + " different items");
        }
    }

    /** All lines must share one currency so the subtotal is meaningful. */
    private void requireSameCurrency(List<CartItem> otherItems, String currency) {
        boolean mixed = otherItems.stream().anyMatch(i -> !i.currency().equals(currency));
        if (mixed) {
            throw new BusinessRuleViolationException("CURRENCY_MISMATCH",
                    "All items in a cart must use the same currency");
        }
    }
}
