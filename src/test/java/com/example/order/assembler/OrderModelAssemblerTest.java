package com.example.order.assembler;

import com.example.order.dto.OrderResponse;
import com.example.order.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OrderModelAssembler HATEOAS functionality.
 * Verifies that hypermedia links are correctly added based on order state.
 */
@DisplayName("OrderModelAssembler Tests")
class OrderModelAssemblerTest {

    private OrderModelAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new OrderModelAssembler();
    }

    @Test
    @DisplayName("Should add self link to order")
    void shouldAddSelfLink() {
        // Given
        OrderResponse order = createTestOrder(OrderStatus.PENDING);

        // When
        OrderResponse result = assembler.toModel(order);

        // Then
        assertThat(result.getLinks()).isNotEmpty();
        assertThat(result.getLink("self")).isPresent();
        assertThat(result.getLink("self").get().getHref()).contains("/api/orders/" + order.getId());
    }

    @Test
    @DisplayName("Should add orders collection link")
    void shouldAddOrdersCollectionLink() {
        // Given
        OrderResponse order = createTestOrder(OrderStatus.PENDING);

        // When
        OrderResponse result = assembler.toModel(order);

        // Then
        assertThat(result.getLink("orders")).isPresent();
        assertThat(result.getLink("orders").get().getHref()).contains("/api/orders");
    }

    @Test
    @DisplayName("Should add customer-orders link")
    void shouldAddCustomerOrdersLink() {
        // Given
        OrderResponse order = createTestOrder(OrderStatus.PENDING);

        // When
        OrderResponse result = assembler.toModel(order);

        // Then
        assertThat(result.getLink("customer-orders")).isPresent();
        assertThat(result.getLink("customer-orders").get().getHref())
                .contains("/api/orders/customer/" + order.getCustomerId());
    }

    @Test
    @DisplayName("Should add process and cancel links for PENDING order")
    void shouldAddProcessAndCancelLinksForPendingOrder() {
        // Given
        OrderResponse order = createTestOrder(OrderStatus.PENDING);

        // When
        OrderResponse result = assembler.toModel(order);

        // Then
        assertThat(result.getLink("process")).isPresent()
                .hasValueSatisfying(link -> assertThat(link.getHref()).contains("/api/orders/" + order.getId() + "/status"));

        assertThat(result.getLink("cancel")).isPresent()
                .hasValueSatisfying(link -> assertThat(link.getHref()).contains("/api/orders/" + order.getId() + "/cancel"));
    }

    @Test
    @DisplayName("Should add ship and cancel links for PROCESSING order")
    void shouldAddShipAndCancelLinksForProcessingOrder() {
        // Given
        OrderResponse order = createTestOrder(OrderStatus.PROCESSING);

        // When
        OrderResponse result = assembler.toModel(order);

        // Then
        assertThat(result.getLink("ship")).isPresent()
                .hasValueSatisfying(link -> assertThat(link.getHref()).contains("/api/orders/" + order.getId() + "/status"));

        assertThat(result.getLink("cancel")).isPresent()
                .hasValueSatisfying(link -> assertThat(link.getHref()).contains("/api/orders/" + order.getId() + "/cancel"));
    }

    @Test
    @DisplayName("Should add only deliver link for SHIPPED order")
    void shouldAddOnlyDeliverLinkForShippedOrder() {
        // Given
        OrderResponse order = createTestOrder(OrderStatus.SHIPPED);

        // When
        OrderResponse result = assembler.toModel(order);

        // Then
        assertThat(result.getLink("deliver")).isPresent()
                .hasValueSatisfying(link -> assertThat(link.getHref()).contains("/api/orders/" + order.getId() + "/status"));

        // Cancel link should not be present for shipped orders
        assertThat(result.getLink("cancel")).isNotPresent();
    }

    @Test
    @DisplayName("Should not add action links for DELIVERED order")
    void shouldNotAddActionLinksForDeliveredOrder() {
        // Given
        OrderResponse order = createTestOrder(OrderStatus.DELIVERED);

        // When
        OrderResponse result = assembler.toModel(order);

        // Then
        // Only navigation links should be present, no action links
        assertThat(result.getLink("self")).isPresent();
        assertThat(result.getLink("orders")).isPresent();
        assertThat(result.getLink("customer-orders")).isPresent();

        // No action links
        assertThat(result.getLink("process")).isNotPresent();
        assertThat(result.getLink("ship")).isNotPresent();
        assertThat(result.getLink("deliver")).isNotPresent();
        assertThat(result.getLink("cancel")).isNotPresent();
    }

    @Test
    @DisplayName("Should not add action links for CANCELLED order")
    void shouldNotAddActionLinksForCancelledOrder() {
        // Given
        OrderResponse order = createTestOrder(OrderStatus.CANCELLED);

        // When
        OrderResponse result = assembler.toModel(order);

        // Then
        // Only navigation links should be present, no action links
        assertThat(result.getLink("self")).isPresent();
        assertThat(result.getLink("orders")).isPresent();
        assertThat(result.getLink("customer-orders")).isPresent();

        // No action links
        assertThat(result.getLink("process")).isNotPresent();
        assertThat(result.getLink("ship")).isNotPresent();
        assertThat(result.getLink("deliver")).isNotPresent();
        assertThat(result.getLink("cancel")).isNotPresent();
    }

    @Test
    @DisplayName("Should convert collection to model with links")
    void shouldConvertCollectionToModelWithLinks() {
        // Given
        List<OrderResponse> orders = List.of(
                createTestOrder(OrderStatus.PENDING),
                createTestOrder(OrderStatus.PROCESSING),
                createTestOrder(OrderStatus.SHIPPED)
        );

        // When
        CollectionModel<OrderResponse> result = assembler.toCollectionModel(orders);

        // Then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getLink("self")).isPresent();

        // Verify each order has links
        result.getContent().forEach(order -> {
            assertThat(order.getLink("self")).isPresent();
            assertThat(order.getLink("orders")).isPresent();
            assertThat(order.getLink("customer-orders")).isPresent();
        });
    }

    @Test
    @DisplayName("Should maintain order data when adding links")
    void shouldMaintainOrderDataWhenAddingLinks() {
        // Given
        OrderResponse order = createTestOrder(OrderStatus.PENDING);
        UUID originalId = order.getId();
        String originalCustomerId = order.getCustomerId();
        OrderStatus originalStatus = order.getStatus();
        BigDecimal originalAmount = order.getTotalAmount();

        // When
        OrderResponse result = assembler.toModel(order);

        // Then
        assertThat(result.getId()).isEqualTo(originalId);
        assertThat(result.getCustomerId()).isEqualTo(originalCustomerId);
        assertThat(result.getStatus()).isEqualTo(originalStatus);
        assertThat(result.getTotalAmount()).isEqualTo(originalAmount);
    }

    @Test
    @DisplayName("Should add correct number of links for each state")
    void shouldAddCorrectNumberOfLinksForEachState() {
        // PENDING: self, orders, customer-orders, process, cancel = 5
        OrderResponse pending = assembler.toModel(createTestOrder(OrderStatus.PENDING));
        assertThat(pending.getLinks()).hasSize(5);

        // PROCESSING: self, orders, customer-orders, ship, cancel = 5
        OrderResponse processing = assembler.toModel(createTestOrder(OrderStatus.PROCESSING));
        assertThat(processing.getLinks()).hasSize(5);

        // SHIPPED: self, orders, customer-orders, deliver = 4
        OrderResponse shipped = assembler.toModel(createTestOrder(OrderStatus.SHIPPED));
        assertThat(shipped.getLinks()).hasSize(4);

        // DELIVERED: self, orders, customer-orders = 3
        OrderResponse delivered = assembler.toModel(createTestOrder(OrderStatus.DELIVERED));
        assertThat(delivered.getLinks()).hasSize(3);

        // CANCELLED: self, orders, customer-orders = 3
        OrderResponse cancelled = assembler.toModel(createTestOrder(OrderStatus.CANCELLED));
        assertThat(cancelled.getLinks()).hasSize(3);
    }

    private OrderResponse createTestOrder(OrderStatus status) {
        return OrderResponse.builder()
                .id(UUID.randomUUID())
                .customerId("CUST" + System.nanoTime())
                .status(status)
                .totalAmount(new BigDecimal("299.99"))
                .items(List.of(
                        OrderResponse.Item.builder()
                                .productId("PROD123")
                                .quantity(2)
                                .unitPrice(new BigDecimal("149.99"))
                                .lineTotal(new BigDecimal("299.98"))
                                .build()
                ))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}

