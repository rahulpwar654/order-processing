package com.ecommerce.orderprocessing;

import com.ecommerce.orderprocessing.model.Customer;
import com.ecommerce.orderprocessing.model.Order;
import com.ecommerce.orderprocessing.model.OrderItem;
import com.ecommerce.orderprocessing.model.OrderStatus;
import com.ecommerce.orderprocessing.repository.InMemoryOrderRepository;
import com.ecommerce.orderprocessing.repository.OrderRepository;
import com.ecommerce.orderprocessing.service.OrderService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Main application demonstrating the Order Processing System.
 */
public class OrderProcessingApplication {

    public static void main(String[] args) {
        System.out.println("=== Order Processing System Demo ===\n");

        // Initialize repository and service
        OrderRepository repository = new InMemoryOrderRepository();
        OrderService orderService = new OrderService(repository);

        // Create sample customers
        Customer customer1 = new Customer("CUST-001", "John Doe", "john@example.com", "123 Main St");
        Customer customer2 = new Customer("CUST-002", "Jane Smith", "jane@example.com", "456 Oak Ave");

        // Create sample order items
        List<OrderItem> items1 = Arrays.asList(
                new OrderItem("PROD-001", "Laptop", 1, new BigDecimal("999.99")),
                new OrderItem("PROD-002", "Mouse", 2, new BigDecimal("25.50"))
        );

        List<OrderItem> items2 = Arrays.asList(
                new OrderItem("PROD-003", "Keyboard", 1, new BigDecimal("79.99")),
                new OrderItem("PROD-004", "Monitor", 2, new BigDecimal("299.99"))
        );

        System.out.println("1. Creating orders...");
        Order order1 = orderService.createOrder(customer1, items1);
        Order order2 = orderService.createOrder(customer2, items2);
        
        System.out.println("   - Created order: " + order1.getOrderId() + " for " + customer1.getName());
        System.out.println("     Status: " + order1.getStatus() + ", Total: $" + order1.getTotalAmount());
        System.out.println("   - Created order: " + order2.getOrderId() + " for " + customer2.getName());
        System.out.println("     Status: " + order2.getStatus() + ", Total: $" + order2.getTotalAmount());

        System.out.println("\n2. Updating order status...");
        orderService.updateOrderStatus(order1.getOrderId(), OrderStatus.CONFIRMED);
        orderService.updateOrderStatus(order1.getOrderId(), OrderStatus.PROCESSING);
        Order updatedOrder1 = orderService.updateOrderStatus(order1.getOrderId(), OrderStatus.SHIPPED);
        System.out.println("   - Order " + order1.getOrderId() + " status: " + updatedOrder1.getStatus());

        System.out.println("\n3. Retrieving order by ID...");
        Order retrievedOrder = orderService.getOrder(order1.getOrderId());
        System.out.println("   - Retrieved order: " + retrievedOrder.getOrderId());
        System.out.println("     Customer: " + retrievedOrder.getCustomer().getName());
        System.out.println("     Items: " + retrievedOrder.getItems().size());
        System.out.println("     Total: $" + retrievedOrder.getTotalAmount());

        System.out.println("\n4. Getting orders by customer...");
        List<Order> customer1Orders = orderService.getOrdersByCustomer(customer1.getId());
        System.out.println("   - Customer " + customer1.getName() + " has " + customer1Orders.size() + " order(s)");

        System.out.println("\n5. Getting orders by status...");
        List<Order> pendingOrders = orderService.getOrdersByStatus(OrderStatus.PENDING);
        List<Order> shippedOrders = orderService.getOrdersByStatus(OrderStatus.SHIPPED);
        System.out.println("   - Pending orders: " + pendingOrders.size());
        System.out.println("   - Shipped orders: " + shippedOrders.size());

        System.out.println("\n6. Cancelling an order...");
        Order cancelledOrder = orderService.cancelOrder(order2.getOrderId());
        System.out.println("   - Order " + order2.getOrderId() + " status: " + cancelledOrder.getStatus());

        System.out.println("\n7. Getting all orders...");
        List<Order> allOrders = orderService.getAllOrders();
        System.out.println("   - Total orders in system: " + allOrders.size());
        for (Order order : allOrders) {
            System.out.println("     * " + order.getOrderId() + " - " + order.getStatus() + " - $" + order.getTotalAmount());
        }

        System.out.println("\n=== Demo completed successfully! ===");
    }
}
