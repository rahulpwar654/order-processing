package com.ecommerce.orderprocessing.exception;

/**
 * Exception thrown when an order is not found in the system.
 */
public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(String orderId) {
        super("Order not found with ID: " + orderId);
    }
}
