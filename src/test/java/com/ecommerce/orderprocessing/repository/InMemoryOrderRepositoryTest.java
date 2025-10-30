package com.ecommerce.orderprocessing.repository;

import com.ecommerce.orderprocessing.model.Customer;
import com.ecommerce.orderprocessing.model.Order;
import com.ecommerce.orderprocessing.model.OrderItem;
import com.ecommerce.orderprocessing.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryOrderRepositoryTest {

    private OrderRepository repository;
    private Customer customer;
    private List<OrderItem> items;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
        customer = new Customer("CUST-001", "John Doe", "john@example.com", "123 Main St");
        items = Arrays.asList(
                new OrderItem("PROD-001", "Product 1", 2, new BigDecimal("10.00")),
                new OrderItem("PROD-002", "Product 2", 1, new BigDecimal("20.00"))
        );
    }

    @Test
    void testSaveOrder() {
        Order order = new Order("ORD-001", customer, items);
        
        Order savedOrder = repository.save(order);
        
        assertNotNull(savedOrder);
        assertEquals("ORD-001", savedOrder.getOrderId());
    }

    @Test
    void testFindById() {
        Order order = new Order("ORD-001", customer, items);
        repository.save(order);
        
        Optional<Order> foundOrder = repository.findById("ORD-001");
        
        assertTrue(foundOrder.isPresent());
        assertEquals("ORD-001", foundOrder.get().getOrderId());
    }

    @Test
    void testFindByIdNotFound() {
        Optional<Order> foundOrder = repository.findById("ORD-999");
        
        assertFalse(foundOrder.isPresent());
    }

    @Test
    void testFindByCustomerId() {
        Order order1 = new Order("ORD-001", customer, items);
        Order order2 = new Order("ORD-002", customer, items);
        Customer anotherCustomer = new Customer("CUST-002", "Jane Doe", "jane@example.com", "456 Oak St");
        Order order3 = new Order("ORD-003", anotherCustomer, items);
        
        repository.save(order1);
        repository.save(order2);
        repository.save(order3);
        
        List<Order> customerOrders = repository.findByCustomerId("CUST-001");
        
        assertEquals(2, customerOrders.size());
    }

    @Test
    void testFindByStatus() {
        Order order1 = new Order("ORD-001", customer, items);
        order1.setStatus(OrderStatus.CONFIRMED);
        Order order2 = new Order("ORD-002", customer, items);
        order2.setStatus(OrderStatus.CONFIRMED);
        Order order3 = new Order("ORD-003", customer, items);
        
        repository.save(order1);
        repository.save(order2);
        repository.save(order3);
        
        List<Order> confirmedOrders = repository.findByStatus(OrderStatus.CONFIRMED);
        
        assertEquals(2, confirmedOrders.size());
    }

    @Test
    void testFindAll() {
        Order order1 = new Order("ORD-001", customer, items);
        Order order2 = new Order("ORD-002", customer, items);
        
        repository.save(order1);
        repository.save(order2);
        
        List<Order> allOrders = repository.findAll();
        
        assertEquals(2, allOrders.size());
    }

    @Test
    void testDeleteById() {
        Order order = new Order("ORD-001", customer, items);
        repository.save(order);
        
        boolean deleted = repository.deleteById("ORD-001");
        
        assertTrue(deleted);
        assertFalse(repository.findById("ORD-001").isPresent());
    }

    @Test
    void testDeleteByIdNotFound() {
        boolean deleted = repository.deleteById("ORD-999");
        
        assertFalse(deleted);
    }
}
