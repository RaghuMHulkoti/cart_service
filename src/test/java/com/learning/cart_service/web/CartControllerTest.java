package com.learning.cart_service.web;

import com.learning.cart_service.core.domain.CartView;
import com.learning.cart_service.core.processor.AddCartItemProcessor;
import com.learning.cart_service.core.processor.ClearCartProcessor;
import com.learning.cart_service.core.processor.GetCartProcessor;
import com.learning.cart_service.core.processor.GetCartProcessorResponse;
import com.learning.cart_service.core.processor.MergeCartProcessor;
import com.learning.cart_service.core.processor.RemoveCartItemProcessor;
import com.learning.cart_service.core.processor.UpdateCartItemProcessor;
import com.learning.cart_service.exception.BusinessRuleViolationException;
import com.learning.cart_service.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartControllerTest {

    final UUID user = UUID.randomUUID();
    AddCartItemProcessor add = Mockito.mock(AddCartItemProcessor.class);
    GetCartProcessor getCart = Mockito.mock(GetCartProcessor.class);
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        var controller = new CartController(getCart, add, Mockito.mock(UpdateCartItemProcessor.class),
                Mockito.mock(RemoveCartItemProcessor.class), Mockito.mock(ClearCartProcessor.class),
                Mockito.mock(MergeCartProcessor.class));
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void getWithoutUserHeaderIs401() throws Exception {
        mvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.body.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void getReturnsEnvelope() throws Exception {
        when(getCart.process(any())).thenReturn(new GetCartProcessorResponse(
                new CartView(user, List.of(), 0, BigDecimal.ZERO, null, false)));

        mvc.perform(get("/api/v1/cart").header("X-User-Id", user.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.body.cartId").value(user.toString()));
    }

    @Test
    void addValidatesBody() throws Exception {
        mvc.perform(post("/api/v1/cart/items").header("X-User-Id", user.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.body.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void businessRuleIs422WithItsCode() throws Exception {
        when(add.process(any())).thenThrow(new BusinessRuleViolationException("OUT_OF_STOCK", "Item is out of stock"));

        mvc.perform(post("/api/v1/cart/items").header("X-User-Id", user.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":1}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.body.errorCode").value("OUT_OF_STOCK"));
    }

    @Test
    void clearIs204() throws Exception {
        mvc.perform(delete("/api/v1/cart").header("X-User-Id", user.toString()))
                .andExpect(status().isNoContent());
    }
}
