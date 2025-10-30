package com.ecommerce.orderprocessing.model;

/**
 * Enum representing the status of an order in the system.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
