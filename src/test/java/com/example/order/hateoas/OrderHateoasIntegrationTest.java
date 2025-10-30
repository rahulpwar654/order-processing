package com.example.order.hateoas;

import com.example.order.dto.OrderCreateRequest;
import com.example.order.dto.OrderItemRequest;
import com.example.order.dto.OrderStatusUpdateRequest;
import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for HATEOAS implementation in OrderController.
 * Tests verify that hypermedia links are correctly included in API responses
 * and that the links change based on the order's state.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("HATEOAS Integration Tests")
class OrderHateoasIntegrationTest {

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
    @DisplayName("POST /api/orders should return order with HATEOAS links")
    void createOrder_ShouldReturnOrderWithHateoasLinks() throws Exception {
        // Given
        OrderCreateRequest request = OrderCreateRequest.builder()
                .customerId("CUST123")
                .items(List.of(
                        OrderItemRequest.builder()
                                .productId("PROD456")
                                .quantity(2)
                                .unitPrice(new BigDecimal("149.99"))
                                .build()
                ))
                .build();

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.self.href").value(containsString("/api/orders/")))
                .andExpect(jsonPath("$._links.orders.href").exists())
                .andExpect(jsonPath("$._links.orders.href").value(containsString("/api/orders")))
                .andExpect(jsonPath("$._links['customer-orders'].href").exists())
                .andExpect(jsonPath("$._links['customer-orders'].href").value(containsString("/api/orders/customer/CUST123")))
                .andExpect(jsonPath("$._links.process.href").exists())
                .andExpect(jsonPath("$._links.cancel.href").exists());
    }

    @Test
    @DisplayName("GET /api/orders/{id} should return order with HATEOAS links")
    void getOrder_ShouldReturnOrderWithHateoasLinks() throws Exception {
        // Given
        Order order = createAndSaveOrder("CUST123", OrderStatus.PENDING);

        // When & Then
        mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().toString()))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.orders.href").exists())
                .andExpect(jsonPath("$._links['customer-orders'].href").exists())
                .andExpect(jsonPath("$._links.process.href").exists())
                .andExpect(jsonPath("$._links.cancel.href").exists());
    }

    @Test
    @DisplayName("GET /api/orders should return paged collection with HATEOAS links")
    void listOrders_ShouldReturnPagedCollectionWithHateoasLinks() throws Exception {
        // Given
        createAndSaveOrder("CUST123", OrderStatus.PENDING);
        createAndSaveOrder("CUST456", OrderStatus.PROCESSING);

        // When & Then
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.orderResponseList").isArray())
                .andExpect(jsonPath("$._embedded.orderResponseList[0]._links.self").exists())
                .andExpect(jsonPath("$._embedded.orderResponseList[0]._links.orders").exists())
                .andExpect(jsonPath("$._embedded.orderResponseList[0]._links['customer-orders']").exists())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/orders/customer/{customerId} should return paged collection with HATEOAS links")
    void getOrdersByCustomer_ShouldReturnPagedCollectionWithHateoasLinks() throws Exception {
        // Given
        String customerId = "CUST123";
        createAndSaveOrder(customerId, OrderStatus.PENDING);
        createAndSaveOrder(customerId, OrderStatus.PROCESSING);
        createAndSaveOrder("OTHER_CUSTOMER", OrderStatus.PENDING);

        // When & Then
        mockMvc.perform(get("/api/orders/customer/{customerId}", customerId)
                        .param("page", "0")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.orderResponseList").isArray())
                .andExpect(jsonPath("$._embedded.orderResponseList", hasSize(2)))
                .andExpect(jsonPath("$._embedded.orderResponseList[0]._links.self").exists())
                .andExpect(jsonPath("$._embedded.orderResponseList[0]._links.orders").exists())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    @DisplayName("PENDING order should have process and cancel links")
    void pendingOrder_ShouldHaveProcessAndCancelLinks() throws Exception {
        // Given
        Order order = createAndSaveOrder("CUST123", OrderStatus.PENDING);

        // When & Then
        mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$._links.process.href").exists())
                .andExpect(jsonPath("$._links.cancel.href").exists())
                .andExpect(jsonPath("$._links.ship").doesNotExist())
                .andExpect(jsonPath("$._links.deliver").doesNotExist());
    }

    @Test
    @DisplayName("PROCESSING order should have ship and cancel links")
    void processingOrder_ShouldHaveShipAndCancelLinks() throws Exception {
        // Given
        Order order = createAndSaveOrder("CUST123", OrderStatus.PROCESSING);

        // When & Then
        mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$._links.ship.href").exists())
                .andExpect(jsonPath("$._links.cancel.href").exists())
                .andExpect(jsonPath("$._links.process").doesNotExist())
                .andExpect(jsonPath("$._links.deliver").doesNotExist());
    }

    @Test
    @DisplayName("SHIPPED order should have only deliver link")
    void shippedOrder_ShouldHaveOnlyDeliverLink() throws Exception {
        // Given
        Order order = createAndSaveOrder("CUST123", OrderStatus.SHIPPED);

        // When & Then
        mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$._links.deliver.href").exists())
                .andExpect(jsonPath("$._links.process").doesNotExist())
                .andExpect(jsonPath("$._links.ship").doesNotExist())
                .andExpect(jsonPath("$._links.cancel").doesNotExist());
    }

    @Test
    @DisplayName("DELIVERED order should have no action links")
    void deliveredOrder_ShouldHaveNoActionLinks() throws Exception {
        // Given
        Order order = createAndSaveOrder("CUST123", OrderStatus.DELIVERED);

        // When & Then
        mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.orders.href").exists())
                .andExpect(jsonPath("$._links['customer-orders'].href").exists())
                .andExpect(jsonPath("$._links.process").doesNotExist())
                .andExpect(jsonPath("$._links.ship").doesNotExist())
                .andExpect(jsonPath("$._links.deliver").doesNotExist())
                .andExpect(jsonPath("$._links.cancel").doesNotExist());
    }

    @Test
    @DisplayName("CANCELLED order should have no action links")
    void cancelledOrder_ShouldHaveNoActionLinks() throws Exception {
        // Given
        Order order = createAndSaveOrder("CUST123", OrderStatus.CANCELLED);

        // When & Then
        mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.orders.href").exists())
                .andExpect(jsonPath("$._links['customer-orders'].href").exists())
                .andExpect(jsonPath("$._links.process").doesNotExist())
                .andExpect(jsonPath("$._links.ship").doesNotExist())
                .andExpect(jsonPath("$._links.deliver").doesNotExist())
                .andExpect(jsonPath("$._links.cancel").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/status should return updated order with new state links")
    void updateStatus_ShouldReturnUpdatedOrderWithNewStateLinks() throws Exception {
        // Given
        Order order = createAndSaveOrder("CUST123", OrderStatus.PENDING);
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder()
                .status(OrderStatus.PROCESSING)
                .build();

        // When & Then
        mockMvc.perform(patch("/api/orders/{id}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$._links.ship.href").exists())
                .andExpect(jsonPath("$._links.cancel.href").exists())
                .andExpect(jsonPath("$._links.process").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/cancel should return cancelled order with no action links")
    void cancelOrder_ShouldReturnCancelledOrderWithNoActionLinks() throws Exception {
        // Given
        Order order = createAndSaveOrder("CUST123", OrderStatus.PENDING);

        // When & Then
        mockMvc.perform(post("/api/orders/{id}/cancel", order.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.orders.href").exists())
                .andExpect(jsonPath("$._links['customer-orders'].href").exists())
                .andExpect(jsonPath("$._links.process").doesNotExist())
                .andExpect(jsonPath("$._links.ship").doesNotExist())
                .andExpect(jsonPath("$._links.deliver").doesNotExist())
                .andExpect(jsonPath("$._links.cancel").doesNotExist());
    }

    @Test
    @DisplayName("Links should contain correct order ID")
    void links_ShouldContainCorrectOrderId() throws Exception {
        // Given
        Order order = createAndSaveOrder("CUST123", OrderStatus.PENDING);

        // When & Then
        MvcResult result = mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andReturn();

        String selfLink = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("_links").get("self").get("href").asText();

        assertThat(selfLink).contains(order.getId().toString());
    }

    @Test
    @DisplayName("Pagination links should be present in collection responses")
    void paginationLinks_ShouldBePresentInCollectionResponses() throws Exception {
        // Given - Create enough orders to span multiple pages
        for (int i = 0; i < 25; i++) {
            createAndSaveOrder("CUST" + i, OrderStatus.PENDING);
        }

        // When & Then - First page
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.first.href").exists())
                .andExpect(jsonPath("$._links.next.href").exists())
                .andExpect(jsonPath("$._links.last.href").exists())
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(25))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    private Order createAndSaveOrder(String customerId, OrderStatus status) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("299.99"));
        return orderRepository.save(order);
    }
}

