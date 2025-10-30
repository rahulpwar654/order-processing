package com.ecommerce.orderprocessing.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {
    
    private Customer customer;
    private List<OrderItem> items;

    @BeforeEach
    void setUp() {
        customer = new Customer("CUST-001", "John Doe", "john@example.com", "123 Main St");
        items = Arrays.asList(
                new OrderItem("PROD-001", "Product 1", 2, new BigDecimal("10.00")),
                new OrderItem("PROD-002", "Product 2", 1, new BigDecimal("20.00"))
        );
    }

    @Test
    void testCreateOrder() {
        Order order = new Order("ORD-001", customer, items);
        
        assertEquals("ORD-001", order.getOrderId());
        assertEquals(customer, order.getCustomer());
        assertEquals(2, order.getItems().size());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getUpdatedAt());
    }

    @Test
    void testCreateOrderWithEmptyItems() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Order("ORD-001", customer, Collections.emptyList());
        });
    }

    @Test
    void testCreateOrderWithNullItems() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Order("ORD-001", customer, null);
        });
    }

    @Test
    void testGetTotalAmount() {
        Order order = new Order("ORD-001", customer, items);
        
        // 2 * 10.00 + 1 * 20.00 = 40.00
        assertEquals(new BigDecimal("40.00"), order.getTotalAmount());
    }

    @Test
    void testSetStatus() {
        Order order = new Order("ORD-001", customer, items);
        
        order.setStatus(OrderStatus.CONFIRMED);
        
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void testGetItemsReturnsUnmodifiableList() {
        Order order = new Order("ORD-001", customer, items);
        
        assertThrows(UnsupportedOperationException.class, () -> {
            order.getItems().add(new OrderItem("PROD-003", "Product 3", 1, new BigDecimal("5.00")));
        });
    }
}
