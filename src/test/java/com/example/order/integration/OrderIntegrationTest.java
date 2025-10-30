package com.example.order.integration;

import com.example.order.dto.*;
import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@org.springframework.test.context.ActiveProfiles("test")
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void fullOrderLifecycle_CreateGetUpdateCancel() throws Exception {
        // 1. Create order
        OrderCreateRequest createRequest = OrderCreateRequest.builder()
                .customerId("cust-integration-test")
                .items(Arrays.asList(
                        OrderItemRequest.builder()
                                .productId("sku-100")
                                .quantity(3)
                                .unitPrice(new BigDecimal("15.99"))
                                .build(),
                        OrderItemRequest.builder()
                                .productId("sku-200")
                                .quantity(1)
                                .unitPrice(new BigDecimal("25.50"))
                                .build()
                ))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(73.47))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        OrderResponse createdOrder = objectMapper.readValue(responseBody, OrderResponse.class);
        UUID orderId = createdOrder.getId();

        // 2. Get order by ID
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value("cust-integration-test"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // 3. List all orders
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(orderId.toString()));

        // 4. Cancel order
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceledAt").exists());

        // 5. Verify order is canceled
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceledAt").exists());
    }

    @Test
    void statusTransition_ProcessingToShippedToDelivered() throws Exception {
        // Create order in PROCESSING status (simulate scheduler already ran)
        Order order = Order.builder()
                .customerId("cust-status-test")
                .status(OrderStatus.PROCESSING)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        Order savedOrder = orderRepository.save(order);
        UUID orderId = savedOrder.getId();

        // 1. Update from PROCESSING to SHIPPED
        OrderStatusUpdateRequest shippedRequest = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.SHIPPED)
                .build();

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shippedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        // 2. Update from SHIPPED to DELIVERED
        OrderStatusUpdateRequest deliveredRequest = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.DELIVERED)
                .build();

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deliveredRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // 3. Try to update DELIVERED order (should fail)
        OrderStatusUpdateRequest invalidRequest = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.SHIPPED)
                .build();

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order already delivered"));
    }

    @Test
    void invalidStatusTransitions_ShouldReturnConflict() throws Exception {
        // Create order in PROCESSING status
        Order order = Order.builder()
                .customerId("cust-invalid-transition")
                .status(OrderStatus.PROCESSING)
                .totalAmount(new BigDecimal("50.00"))
                .build();
        Order savedOrder = orderRepository.save(order);
        UUID orderId = savedOrder.getId();

        // Try to jump from PROCESSING to DELIVERED (should fail)
        OrderStatusUpdateRequest invalidRequest = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.DELIVERED)
                .build();

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only allowed transition from PROCESSING is to SHIPPED"));
    }

    @Test
    void cancelOrder_ShouldFailWhenNotPending() throws Exception {
        // Create order in PROCESSING status
        Order order = Order.builder()
                .customerId("cust-cancel-test")
                .status(OrderStatus.PROCESSING)
                .totalAmount(new BigDecimal("75.00"))
                .build();
        Order savedOrder = orderRepository.save(order);
        UUID orderId = savedOrder.getId();

        // Try to cancel (should fail)
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Can only cancel orders in PENDING status"));
    }

    @Test
    void listOrders_WithStatusFilter() throws Exception {
        // Create orders with different statuses
        Order pendingOrder = Order.builder()
                .customerId("cust-1")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("10.00"))
                .build();

        Order processingOrder = Order.builder()
                .customerId("cust-2")
                .status(OrderStatus.PROCESSING)
                .totalAmount(new BigDecimal("20.00"))
                .build();

        Order shippedOrder = Order.builder()
                .customerId("cust-3")
                .status(OrderStatus.SHIPPED)
                .totalAmount(new BigDecimal("30.00"))
                .build();

        orderRepository.saveAll(Arrays.asList(pendingOrder, processingOrder, shippedOrder));

        // List all orders
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));

        // Filter by PENDING
        mockMvc.perform(get("/api/orders").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        // Filter by PROCESSING
        mockMvc.perform(get("/api/orders").param("status", "PROCESSING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("PROCESSING"));

        // Filter by SHIPPED
        mockMvc.perform(get("/api/orders").param("status", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("SHIPPED"));
    }

    @Test
    void pagination_ShouldWorkCorrectly() throws Exception {
        // Create 25 orders
        for (int i = 0; i < 25; i++) {
            Order order = Order.builder()
                    .customerId("cust-" + i)
                    .status(OrderStatus.PENDING)
                    .totalAmount(new BigDecimal("10.00"))
                    .build();
            orderRepository.save(order);
        }

        // First page (default size 20)
        mockMvc.perform(get("/api/orders").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2));

        // Second page
        mockMvc.perform(get("/api/orders").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(25));

        // Custom page size
        mockMvc.perform(get("/api/orders").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void createOrder_ValidationFailures() throws Exception {
        // Empty customer ID
        OrderCreateRequest emptyCustomer = OrderCreateRequest.builder()
                .customerId("")
                .items(Arrays.asList(
                        OrderItemRequest.builder()
                                .productId("sku-1")
                                .quantity(1)
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyCustomer)))
                .andExpect(status().isBadRequest());

        // Zero quantity
        OrderCreateRequest zeroQuantity = OrderCreateRequest.builder()
                .customerId("cust-123")
                .items(Arrays.asList(
                        OrderItemRequest.builder()
                                .productId("sku-1")
                                .quantity(0)
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroQuantity)))
                .andExpect(status().isBadRequest());

        // Negative price
        OrderCreateRequest negativePrice = OrderCreateRequest.builder()
                .customerId("cust-123")
                .items(Arrays.asList(
                        OrderItemRequest.builder()
                                .productId("sku-1")
                                .quantity(1)
                                .unitPrice(new BigDecimal("-10.00"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativePrice)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void orderNotFound_ShouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        // Get non-existent order
        mockMvc.perform(get("/api/orders/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        // Update non-existent order
        OrderStatusUpdateRequest updateRequest = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.SHIPPED)
                .build();

        mockMvc.perform(patch("/api/orders/{id}/status", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        // Cancel non-existent order
        mockMvc.perform(post("/api/orders/{id}/cancel", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void totalAmountCalculation_ShouldBeAccurate() throws Exception {
        OrderCreateRequest request = OrderCreateRequest.builder()
                .customerId("cust-calc-test")
                .items(Arrays.asList(
                        OrderItemRequest.builder()
                                .productId("sku-1")
                                .quantity(3)
                                .unitPrice(new BigDecimal("10.99"))
                                .build(),
                        OrderItemRequest.builder()
                                .productId("sku-2")
                                .quantity(2)
                                .unitPrice(new BigDecimal("5.50"))
                                .build(),
                        OrderItemRequest.builder()
                                .productId("sku-3")
                                .quantity(1)
                                .unitPrice(new BigDecimal("99.99"))
                                .build()
                ))
                .build();

        // Expected: (3 * 10.99) + (2 * 5.50) + (1 * 99.99) = 32.97 + 11.00 + 99.99 = 143.96
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(143.96))
                .andExpect(jsonPath("$.items[0].lineTotal").value(32.97))
                .andExpect(jsonPath("$.items[1].lineTotal").value(11.00))
                .andExpect(jsonPath("$.items[2].lineTotal").value(99.99));
    }
}

