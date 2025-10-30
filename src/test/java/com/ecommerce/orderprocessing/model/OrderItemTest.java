package com.ecommerce.orderprocessing.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void testCreateOrderItem() {
        OrderItem item = new OrderItem("PROD-001", "Test Product", 5, new BigDecimal("10.50"));
        
        assertEquals("PROD-001", item.getProductId());
        assertEquals("Test Product", item.getProductName());
        assertEquals(5, item.getQuantity());
        assertEquals(new BigDecimal("10.50"), item.getUnitPrice());
    }

    @Test
    void testGetTotalPrice() {
        OrderItem item = new OrderItem("PROD-001", "Test Product", 3, new BigDecimal("10.00"));
        
        assertEquals(new BigDecimal("30.00"), item.getTotalPrice());
    }

    @Test
    void testCreateOrderItemWithNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OrderItem("PROD-001", "Test Product", -1, new BigDecimal("10.00"));
        });
    }

    @Test
    void testCreateOrderItemWithZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OrderItem("PROD-001", "Test Product", 0, new BigDecimal("10.00"));
        });
    }

    @Test
    void testCreateOrderItemWithNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OrderItem("PROD-001", "Test Product", 1, new BigDecimal("-10.00"));
        });
    }
}
