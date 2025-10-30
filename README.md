# Order Processing System

A comprehensive E-commerce Order Processing System backend built with Java.

## Overview

This system provides a robust backend solution for handling e-commerce orders with features for creating orders, tracking their status, and managing order lifecycle from creation to delivery or cancellation.

## Features

- **Order Management**: Create, retrieve, update, and delete orders
- **Order Status Tracking**: Track orders through their complete lifecycle (PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED)
- **Customer Management**: Associate orders with customers
- **Order Cancellation**: Cancel orders that haven't been delivered
- **Status Validation**: Enforce valid status transitions to maintain data integrity
- **Query Capabilities**: 
  - Find orders by customer ID
  - Find orders by status
  - Retrieve all orders
  - Get specific order by ID

## Architecture

The system follows a layered architecture:

- **Model Layer**: Domain entities (Order, Customer, OrderItem, OrderStatus)
- **Repository Layer**: Data access with in-memory implementation
- **Service Layer**: Business logic and order processing rules
- **Exception Layer**: Custom exceptions for error handling

## Project Structure

```
src/
├── main/
│   └── java/
│       └── com/ecommerce/orderprocessing/
│           ├── model/
│           │   ├── Order.java
│           │   ├── Customer.java
│           │   ├── OrderItem.java
│           │   └── OrderStatus.java
│           ├── repository/
│           │   ├── OrderRepository.java
│           │   └── InMemoryOrderRepository.java
│           ├── service/
│           │   └── OrderService.java
│           ├── exception/
│           │   ├── OrderNotFoundException.java
│           │   └── InvalidOrderStatusException.java
│           └── OrderProcessingApplication.java
└── test/
    └── java/
        └── com/ecommerce/orderprocessing/
            ├── model/
            │   ├── OrderTest.java
            │   └── OrderItemTest.java
            ├── repository/
            │   └── InMemoryOrderRepositoryTest.java
            └── service/
                └── OrderServiceTest.java
```

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

## Building the Project

```bash
mvn clean compile
```

## Running Tests

```bash
mvn test
```

## Running the Demo Application

```bash
mvn compile exec:java -Dexec.mainClass="com.ecommerce.orderprocessing.OrderProcessingApplication"
```

## Usage Examples

### Creating an Order

```java
OrderRepository repository = new InMemoryOrderRepository();
OrderService orderService = new OrderService(repository);

Customer customer = new Customer("CUST-001", "John Doe", "john@example.com", "123 Main St");
List<OrderItem> items = Arrays.asList(
    new OrderItem("PROD-001", "Laptop", 1, new BigDecimal("999.99")),
    new OrderItem("PROD-002", "Mouse", 2, new BigDecimal("25.50"))
);

Order order = orderService.createOrder(customer, items);
```

### Updating Order Status

```java
// Valid status progression
orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
orderService.updateOrderStatus(orderId, OrderStatus.PROCESSING);
orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED);
orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED);
```

### Retrieving Orders

```java
// Get specific order
Order order = orderService.getOrder(orderId);

// Get all orders for a customer
List<Order> customerOrders = orderService.getOrdersByCustomer(customerId);

// Get all orders with a specific status
List<Order> pendingOrders = orderService.getOrdersByStatus(OrderStatus.PENDING);

// Get all orders
List<Order> allOrders = orderService.getAllOrders();
```

### Cancelling an Order

```java
Order cancelledOrder = orderService.cancelOrder(orderId);
```

## Order Status Flow

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
    ↓         ↓            ↓          ↓
        CANCELLED (can cancel before delivery)
```

## Domain Models

### Order
- `orderId`: Unique identifier
- `customer`: Associated customer
- `items`: List of order items
- `status`: Current order status
- `createdAt`: Creation timestamp
- `updatedAt`: Last update timestamp
- `totalAmount`: Calculated total from all items

### Customer
- `id`: Unique identifier
- `name`: Customer name
- `email`: Customer email
- `address`: Delivery address

### OrderItem
- `productId`: Product identifier
- `productName`: Product name
- `quantity`: Order quantity
- `unitPrice`: Price per unit
- `totalPrice`: Calculated total (quantity × unitPrice)

## Testing

The project includes comprehensive unit tests:
- Model tests: 11 tests
- Repository tests: 8 tests
- Service tests: 14 tests

Total: 33 tests ensuring system reliability

## Error Handling

- `OrderNotFoundException`: Thrown when an order ID is not found
- `InvalidOrderStatusException`: Thrown for invalid status transitions
- `IllegalArgumentException`: Thrown for invalid order data (empty items, negative prices, etc.)

## Future Enhancements

- Database integration (replace in-memory repository)
- REST API endpoints
- Authentication and authorization
- Order payment processing
- Inventory management integration
- Email notifications
- Order history and audit trail
- Advanced search and filtering
- Batch order processing

## License

This project is available for educational and commercial use.
