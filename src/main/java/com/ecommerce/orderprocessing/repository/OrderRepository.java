package com.ecommerce.orderprocessing.repository;

import com.ecommerce.orderprocessing.model.Order;
import com.ecommerce.orderprocessing.model.OrderStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Order persistence operations.
 */
public interface OrderRepository {
    
    /**
     * Save an order to the repository.
     * 
     * @param order the order to save
     * @return the saved order
     */
    Order save(Order order);
    
    /**
     * Find an order by its ID.
     * 
     * @param orderId the order ID
     * @return Optional containing the order if found, empty otherwise
     */
    Optional<Order> findById(String orderId);
    
    /**
     * Find all orders by customer ID.
     * 
     * @param customerId the customer ID
     * @return list of orders for the customer
     */
    List<Order> findByCustomerId(String customerId);
    
    /**
     * Find all orders with a specific status.
     * 
     * @param status the order status
     * @return list of orders with the given status
     */
    List<Order> findByStatus(OrderStatus status);
    
    /**
     * Find all orders in the repository.
     * 
     * @return list of all orders
     */
    List<Order> findAll();
    
    /**
     * Delete an order by its ID.
     * 
     * @param orderId the order ID
     * @return true if the order was deleted, false otherwise
     */
    boolean deleteById(String orderId);
}
