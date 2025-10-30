package com.example.order.controller;

import com.example.order.dto.*;
import com.example.order.exception.ConflictException;
import com.example.order.exception.NotFoundException;
import com.example.order.model.OrderStatus;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private UUID orderId;
    private OrderResponse orderResponse;
    private OrderCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        orderResponse = OrderResponse.builder()
                .id(orderId)
                .customerId("cust-123")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("31.00"))
                .items(Arrays.asList(
                        OrderResponse.Item.builder()
                                .productId("sku-1")
                                .quantity(2)
                                .unitPrice(new BigDecimal("10.50"))
                                .lineTotal(new BigDecimal("21.00"))
                                .build(),
                        OrderResponse.Item.builder()
                                .productId("sku-2")
                                .quantity(2)
                                .unitPrice(new BigDecimal("5.00"))
                                .lineTotal(new BigDecimal("10.00"))
                                .build()
                ))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        createRequest = OrderCreateRequest.builder()
                .customerId("cust-123")
                .items(Arrays.asList(
                        OrderItemRequest.builder()
                                .productId("sku-1")
                                .quantity(2)
                                .unitPrice(new BigDecimal("10.50"))
                                .build(),
                        OrderItemRequest.builder()
                                .productId("sku-2")
                                .quantity(2)
                                .unitPrice(new BigDecimal("5.00"))
                                .build()
                ))
                .build();
    }

    @Test
    void create_ShouldReturn201_WhenOrderCreatedSuccessfully() throws Exception {
        // Arrange
        when(orderService.create(any(OrderCreateRequest.class))).thenReturn(orderResponse);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value("cust-123"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(31.00))
                .andExpect(jsonPath("$.items", hasSize(2)));

        verify(orderService, times(1)).create(any(OrderCreateRequest.class));
    }

    @Test
    void create_ShouldReturn400_WhenCustomerIdIsBlank() throws Exception {
        // Arrange
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .customerId("")
                .items(Collections.singletonList(
                        OrderItemRequest.builder()
                                .productId("sku-1")
                                .quantity(2)
                                .unitPrice(new BigDecimal("10.50"))
                                .build()
                ))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).create(any());
    }

    @Test
    void create_ShouldReturn400_WhenItemsEmpty() throws Exception {
        // Arrange
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .customerId("cust-123")
                .items(Collections.emptyList())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).create(any());
    }

    @Test
    void create_ShouldReturn400_WhenQuantityIsZero() throws Exception {
        // Arrange
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .customerId("cust-123")
                .items(Collections.singletonList(
                        OrderItemRequest.builder()
                                .productId("sku-1")
                                .quantity(0)
                                .unitPrice(new BigDecimal("10.50"))
                                .build()
                ))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).create(any());
    }

    @Test
    void create_ShouldReturn400_WhenUnitPriceIsNegative() throws Exception {
        // Arrange
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .customerId("cust-123")
                .items(Collections.singletonList(
                        OrderItemRequest.builder()
                                .productId("sku-1")
                                .quantity(2)
                                .unitPrice(new BigDecimal("-10.50"))
                                .build()
                ))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).create(any());
    }

    @Test
    void get_ShouldReturn200_WhenOrderExists() throws Exception {
        // Arrange
        when(orderService.getById(orderId)).thenReturn(orderResponse);

        // Act & Assert
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value("cust-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService, times(1)).getById(orderId);
    }

    @Test
    void get_ShouldReturn404_WhenOrderNotFound() throws Exception {
        // Arrange
        when(orderService.getById(orderId)).thenThrow(new NotFoundException("Order not found"));

        // Act & Assert
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        verify(orderService, times(1)).getById(orderId);
    }

    @Test
    void list_ShouldReturn200_WithAllOrders() throws Exception {
        // Arrange
        Page<OrderResponse> page = new PageImpl<>(Collections.singletonList(orderResponse));
        when(orderService.list(eq(null), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(orderId.toString()));

        verify(orderService, times(1)).list(eq(null), any());
    }

    @Test
    void list_ShouldReturn200_WithFilteredOrders() throws Exception {
        // Arrange
        Page<OrderResponse> page = new PageImpl<>(Collections.singletonList(orderResponse));
        when(orderService.list(eq(OrderStatus.PENDING), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        verify(orderService, times(1)).list(eq(OrderStatus.PENDING), any());
    }

    @Test
    void list_ShouldUseDefaultPagination_WhenNotProvided() throws Exception {
        // Arrange
        Page<OrderResponse> page = new PageImpl<>(Collections.singletonList(orderResponse));
        when(orderService.list(eq(null), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(orderService, times(1)).list(eq(null), eq(PageRequest.of(0, 20)));
    }

    @Test
    void updateStatus_ShouldReturn200_WhenStatusUpdatedSuccessfully() throws Exception {
        // Arrange
        OrderResponse updatedResponse = OrderResponse.builder()
                .id(orderId)
                .customerId("cust-123")
                .status(OrderStatus.SHIPPED)
                .totalAmount(new BigDecimal("31.00"))
                .items(orderResponse.getItems())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(orderService.updateStatus(eq(orderId), eq(OrderStatus.SHIPPED))).thenReturn(updatedResponse);

        OrderStatusUpdateRequest updateRequest = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.SHIPPED)
                .build();

        // Act & Assert
        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        verify(orderService, times(1)).updateStatus(orderId, OrderStatus.SHIPPED);
    }

    @Test
    void updateStatus_ShouldReturn409_WhenInvalidTransition() throws Exception {
        // Arrange
        when(orderService.updateStatus(eq(orderId), any()))
                .thenThrow(new ConflictException("Invalid status transition"));

        OrderStatusUpdateRequest updateRequest = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.DELIVERED)
                .build();

        // Act & Assert
        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        verify(orderService, times(1)).updateStatus(eq(orderId), any());
    }

    @Test
    void updateStatus_ShouldReturn404_WhenOrderNotFound() throws Exception {
        // Arrange
        when(orderService.updateStatus(eq(orderId), any()))
                .thenThrow(new NotFoundException("Order not found"));

        OrderStatusUpdateRequest updateRequest = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.SHIPPED)
                .build();

        // Act & Assert
        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        verify(orderService, times(1)).updateStatus(eq(orderId), any());
    }

    @Test
    void cancel_ShouldReturn200_WhenOrderCanceledSuccessfully() throws Exception {
        // Arrange
        OrderResponse canceledResponse = OrderResponse.builder()
                .id(orderId)
                .customerId("cust-123")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("31.00"))
                .items(orderResponse.getItems())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .canceledAt(Instant.now())
                .build();

        when(orderService.cancel(orderId)).thenReturn(canceledResponse);

        // Act & Assert
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.canceledAt").exists());

        verify(orderService, times(1)).cancel(orderId);
    }

    @Test
    void cancel_ShouldReturn409_WhenOrderAlreadyCanceled() throws Exception {
        // Arrange
        when(orderService.cancel(orderId))
                .thenThrow(new ConflictException("Order already canceled"));

        // Act & Assert
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Order already canceled"));

        verify(orderService, times(1)).cancel(orderId);
    }

    @Test
    void cancel_ShouldReturn409_WhenOrderNotPending() throws Exception {
        // Arrange
        when(orderService.cancel(orderId))
                .thenThrow(new ConflictException("Can only cancel orders in PENDING status"));

        // Act & Assert
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Can only cancel orders in PENDING status"));

        verify(orderService, times(1)).cancel(orderId);
    }

    @Test
    void cancel_ShouldReturn404_WhenOrderNotFound() throws Exception {
        // Arrange
        when(orderService.cancel(orderId))
                .thenThrow(new NotFoundException("Order not found"));

        // Act & Assert
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        verify(orderService, times(1)).cancel(orderId);
    }
}

