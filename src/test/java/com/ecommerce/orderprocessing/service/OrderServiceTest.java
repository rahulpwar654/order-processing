package com.ecommerce.orderprocessing.service;

import com.ecommerce.orderprocessing.exception.InvalidOrderStatusException;
import com.ecommerce.orderprocessing.exception.OrderNotFoundException;
import com.ecommerce.orderprocessing.model.Customer;
import com.ecommerce.orderprocessing.model.Order;
import com.ecommerce.orderprocessing.model.OrderItem;
import com.ecommerce.orderprocessing.model.OrderStatus;
import com.ecommerce.orderprocessing.repository.InMemoryOrderRepository;
import com.ecommerce.orderprocessing.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    private OrderService orderService;
    private Customer customer;
    private List<OrderItem> items;

    @BeforeEach
    void setUp() {
        OrderRepository repository = new InMemoryOrderRepository();
        orderService = new OrderService(repository);
        customer = new Customer("CUST-001", "John Doe", "john@example.com", "123 Main St");
        items = Arrays.asList(
                new OrderItem("PROD-001", "Product 1", 2, new BigDecimal("10.00")),
                new OrderItem("PROD-002", "Product 2", 1, new BigDecimal("20.00"))
        );
    }

    @Test
    void testCreateOrder() {
        Order order = orderService.createOrder(customer, items);
        
        assertNotNull(order);
        assertNotNull(order.getOrderId());
        assertTrue(order.getOrderId().startsWith("ORD-"));
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(customer, order.getCustomer());
    }

    @Test
    void testGetOrder() {
        Order createdOrder = orderService.createOrder(customer, items);
        
        Order retrievedOrder = orderService.getOrder(createdOrder.getOrderId());
        
        assertNotNull(retrievedOrder);
        assertEquals(createdOrder.getOrderId(), retrievedOrder.getOrderId());
    }

    @Test
    void testGetOrderNotFound() {
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrder("ORD-999");
        });
    }

    @Test
    void testGetOrdersByCustomer() {
        orderService.createOrder(customer, items);
        orderService.createOrder(customer, items);
        
        List<Order> orders = orderService.getOrdersByCustomer("CUST-001");
        
        assertEquals(2, orders.size());
    }

    @Test
    void testGetOrdersByStatus() {
        Order order1 = orderService.createOrder(customer, items);
        Order order2 = orderService.createOrder(customer, items);
        orderService.updateOrderStatus(order1.getOrderId(), OrderStatus.CONFIRMED);
        
        List<Order> pendingOrders = orderService.getOrdersByStatus(OrderStatus.PENDING);
        List<Order> confirmedOrders = orderService.getOrdersByStatus(OrderStatus.CONFIRMED);
        
        assertEquals(1, pendingOrders.size());
        assertEquals(1, confirmedOrders.size());
    }

    @Test
    void testGetAllOrders() {
        orderService.createOrder(customer, items);
        orderService.createOrder(customer, items);
        
        List<Order> allOrders = orderService.getAllOrders();
        
        assertEquals(2, allOrders.size());
    }

    @Test
    void testUpdateOrderStatus() {
        Order order = orderService.createOrder(customer, items);
        
        Order updatedOrder = orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CONFIRMED);
        
        assertEquals(OrderStatus.CONFIRMED, updatedOrder.getStatus());
    }

    @Test
    void testUpdateOrderStatusValidTransitions() {
        Order order = orderService.createOrder(customer, items);
        
        // Valid transition chain
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CONFIRMED);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PROCESSING);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.SHIPPED);
        Order finalOrder = orderService.updateOrderStatus(order.getOrderId(), OrderStatus.DELIVERED);
        
        assertEquals(OrderStatus.DELIVERED, finalOrder.getStatus());
    }

    @Test
    void testUpdateOrderStatusInvalidTransition() {
        Order order = orderService.createOrder(customer, items);
        
        // Invalid: PENDING -> PROCESSING (should go through CONFIRMED first)
        assertThrows(InvalidOrderStatusException.class, () -> {
            orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PROCESSING);
        });
    }

    @Test
    void testCannotUpdateDeliveredOrder() {
        Order order = orderService.createOrder(customer, items);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CONFIRMED);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PROCESSING);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.SHIPPED);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.DELIVERED);
        
        assertThrows(InvalidOrderStatusException.class, () -> {
            orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PROCESSING);
        });
    }

    @Test
    void testCancelOrder() {
        Order order = orderService.createOrder(customer, items);
        
        Order cancelledOrder = orderService.cancelOrder(order.getOrderId());
        
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
    }

    @Test
    void testCannotCancelDeliveredOrder() {
        Order order = orderService.createOrder(customer, items);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CONFIRMED);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.PROCESSING);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.SHIPPED);
        orderService.updateOrderStatus(order.getOrderId(), OrderStatus.DELIVERED);
        
        assertThrows(InvalidOrderStatusException.class, () -> {
            orderService.cancelOrder(order.getOrderId());
        });
    }

    @Test
    void testDeleteOrder() {
        Order order = orderService.createOrder(customer, items);
        
        boolean deleted = orderService.deleteOrder(order.getOrderId());
        
        assertTrue(deleted);
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrder(order.getOrderId());
        });
    }

    @Test
    void testDeleteOrderNotFound() {
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.deleteOrder("ORD-999");
        });
    }
}
