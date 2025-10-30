package com.ecommerce.orderprocessing.service;

import com.ecommerce.orderprocessing.exception.InvalidOrderStatusException;
import com.ecommerce.orderprocessing.exception.OrderNotFoundException;
import com.ecommerce.orderprocessing.model.Customer;
import com.ecommerce.orderprocessing.model.Order;
import com.ecommerce.orderprocessing.model.OrderItem;
import com.ecommerce.orderprocessing.model.OrderStatus;
import com.ecommerce.orderprocessing.repository.OrderRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service class for handling order processing operations.
 */
public class OrderService {
    
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Create a new order.
     * 
     * @param customer the customer placing the order
     * @param items the items in the order
     * @return the created order
     */
    public Order createOrder(Customer customer, List<OrderItem> items) {
        String orderId = generateOrderId();
        Order order = new Order(orderId, customer, items);
        return orderRepository.save(order);
    }

    /**
     * Retrieve an order by its ID.
     * 
     * @param orderId the order ID
     * @return the order
     * @throws OrderNotFoundException if the order is not found
     */
    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /**
     * Get all orders for a specific customer.
     * 
     * @param customerId the customer ID
     * @return list of orders for the customer
     */
    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    /**
     * Get all orders with a specific status.
     * 
     * @param status the order status
     * @return list of orders with the given status
     */
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    /**
     * Get all orders in the system.
     * 
     * @return list of all orders
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Update the status of an order.
     * 
     * @param orderId the order ID
     * @param newStatus the new status
     * @return the updated order
     * @throws OrderNotFoundException if the order is not found
     * @throws InvalidOrderStatusException if the status transition is invalid
     */
    public Order updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = getOrder(orderId);
        
        // Validate status transition
        if (!isValidStatusTransition(order.getStatus(), newStatus)) {
            throw new InvalidOrderStatusException(
                    "Invalid status transition from " + order.getStatus() + " to " + newStatus);
        }
        
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    /**
     * Cancel an order.
     * 
     * @param orderId the order ID
     * @return the cancelled order
     * @throws OrderNotFoundException if the order is not found
     * @throws InvalidOrderStatusException if the order cannot be cancelled
     */
    public Order cancelOrder(String orderId) {
        Order order = getOrder(orderId);
        
        // Only allow cancellation if order is not already delivered
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusException("Cannot cancel a delivered order");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    /**
     * Delete an order from the system.
     * 
     * @param orderId the order ID
     * @return true if the order was deleted
     * @throws OrderNotFoundException if the order is not found
     */
    public boolean deleteOrder(String orderId) {
        if (!orderRepository.findById(orderId).isPresent()) {
            throw new OrderNotFoundException(orderId);
        }
        return orderRepository.deleteById(orderId);
    }

    /**
     * Validate if a status transition is valid.
     * 
     * @param currentStatus the current status
     * @param newStatus the new status
     * @return true if the transition is valid
     */
    private boolean isValidStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.DELIVERED) {
            return false;
        }
        
        if (newStatus == OrderStatus.CANCELLED) {
            return true;
        }
        
        // Define valid transitions
        switch (currentStatus) {
            case PENDING:
                return newStatus == OrderStatus.CONFIRMED;
            case CONFIRMED:
                return newStatus == OrderStatus.PROCESSING;
            case PROCESSING:
                return newStatus == OrderStatus.SHIPPED;
            case SHIPPED:
                return newStatus == OrderStatus.DELIVERED;
            default:
                return false;
        }
    }

    /**
     * Generate a unique order ID.
     * 
     * @return a unique order ID
     */
    private String generateOrderId() {
        return "ORD-" + UUID.randomUUID().toString();
    }
}
