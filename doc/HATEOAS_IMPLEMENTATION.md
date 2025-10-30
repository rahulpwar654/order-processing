# HATEOAS Implementation - Richardson Maturity Model Level 3

## Overview

This document describes the implementation of HATEOAS (Hypermedia as the Engine of Application State) in the Order Processing System, achieving Richardson Maturity Model Level 3. HATEOAS enables clients to navigate the API dynamically through hypermedia links, making the API more discoverable and self-documenting.

## Richardson Maturity Model

The Richardson Maturity Model defines four levels of REST API maturity:

- **Level 0**: The Swamp of POX - Single URI, single HTTP method (typically POST)
- **Level 1**: Resources - Multiple URIs, single HTTP method
- **Level 2**: HTTP Verbs - Multiple URIs, proper use of HTTP methods (GET, POST, PUT, DELETE, PATCH)
- **Level 3**: Hypermedia Controls (HATEOAS) - Responses include links to related resources and available actions

Our implementation achieves **Level 3** by including hypermedia links in all API responses.

## Implementation Components

### 1. Spring HATEOAS Dependency

Added to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-hateoas</artifactId>
</dependency>
```

### 2. OrderResponse DTO

The `OrderResponse` class extends `RepresentationModel<OrderResponse>` to support hypermedia links:

```java
public class OrderResponse extends RepresentationModel<OrderResponse> implements Serializable {
    private UUID id;
    private String customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderResponse.Item> items;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant canceledAt;
}
```

### 3. OrderModelAssembler

The `OrderModelAssembler` implements `RepresentationModelAssembler<OrderResponse, OrderResponse>` and is responsible for adding hypermedia links to order responses.

#### Key Features:

1. **Self Link**: Link to the order itself
2. **Collection Link**: Link to all orders
3. **Customer Orders Link**: Link to all orders for the customer
4. **State-Based Links**: Dynamic links based on the current order status

#### State-Based Hypermedia Controls

The assembler adds different action links based on the order's current state:

| Current Status | Available Actions | Links |
|---------------|-------------------|-------|
| PENDING | Process order, Cancel order | `process`, `cancel` |
| PROCESSING | Ship order, Cancel order | `ship`, `cancel` |
| SHIPPED | Deliver order | `deliver` |
| DELIVERED | No further actions | - |
| CANCELLED | No further actions | - |

### 4. OrderController

The controller uses the `OrderModelAssembler` to add HATEOAS links to all responses:

#### Endpoints

1. **POST /api/orders** - Create a new order
2. **GET /api/orders/{id}** - Get a single order
3. **GET /api/orders** - List all orders (with pagination and filtering)
4. **GET /api/orders/customer/{customerId}** - Get orders by customer (with pagination)
5. **PATCH /api/orders/{id}/status** - Update order status
6. **POST /api/orders/{id}/cancel** - Cancel an order

All endpoints return HATEOAS-enabled responses with hypermedia links.

## Example API Responses

### Single Order Response (PENDING status)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST123",
  "status": "PENDING",
  "totalAmount": 299.99,
  "items": [
    {
      "productId": "PROD456",
      "quantity": 2,
      "unitPrice": 149.99,
      "lineTotal": 299.98
    }
  ],
  "createdAt": "2025-10-30T10:15:30Z",
  "updatedAt": "2025-10-30T10:15:30Z",
  "canceledAt": null,
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000"
    },
    "orders": {
      "href": "http://localhost:8080/api/orders?page=0&size=20"
    },
    "customer-orders": {
      "href": "http://localhost:8080/api/orders/customer/CUST123?page=0&size=20"
    },
    "process": {
      "href": "http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000/status"
    },
    "cancel": {
      "href": "http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000/cancel"
    }
  }
}
```

### Single Order Response (SHIPPED status)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST123",
  "status": "SHIPPED",
  "totalAmount": 299.99,
  "items": [
    {
      "productId": "PROD456",
      "quantity": 2,
      "unitPrice": 149.99,
      "lineTotal": 299.98
    }
  ],
  "createdAt": "2025-10-30T10:15:30Z",
  "updatedAt": "2025-10-30T11:30:00Z",
  "canceledAt": null,
  "_links": {
    "self": {
      "href": "http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000"
    },
    "orders": {
      "href": "http://localhost:8080/api/orders?page=0&size=20"
    },
    "customer-orders": {
      "href": "http://localhost:8080/api/orders/customer/CUST123?page=0&size=20"
    },
    "deliver": {
      "href": "http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000/status"
    }
  }
}
```

Note: The `cancel` link is not present because the order cannot be cancelled once shipped.

### Paged Collection Response

```json
{
  "_embedded": {
    "orderResponseList": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "customerId": "CUST123",
        "status": "PENDING",
        "totalAmount": 299.99,
        "_links": {
          "self": { "href": "http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000" },
          "orders": { "href": "http://localhost:8080/api/orders?page=0&size=20" },
          "customer-orders": { "href": "http://localhost:8080/api/orders/customer/CUST123?page=0&size=20" },
          "process": { "href": "http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000/status" },
          "cancel": { "href": "http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000/cancel" }
        }
      }
    ]
  },
  "_links": {
    "self": { "href": "http://localhost:8080/api/orders?page=0&size=20" },
    "first": { "href": "http://localhost:8080/api/orders?page=0&size=20" },
    "next": { "href": "http://localhost:8080/api/orders?page=1&size=20" },
    "last": { "href": "http://localhost:8080/api/orders?page=4&size=20" }
  },
  "page": {
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "number": 0
  }
}
```

## Benefits of HATEOAS

### 1. Self-Documentation
Clients can discover available actions by examining the links in responses, reducing the need for external documentation.

### 2. Decoupling
Clients don't need to hardcode URLs or know the API structure in advance. They can follow links dynamically.

### 3. State Machine Enforcement
The server controls which actions are available based on resource state, preventing invalid transitions.

### 4. API Evolution
URLs can change without breaking clients, as long as the link relations remain consistent.

### 5. Discoverability
Clients can explore the API by following links, making it more intuitive to use.

## Client Implementation Guidelines

### Following Links

Clients should:
1. Parse the `_links` section of responses
2. Use link relations (e.g., `self`, `cancel`, `process`) to identify actions
3. Follow links by making HTTP requests to the provided `href` values
4. NOT hardcode URLs (except for the entry point)

### Example Client Flow

```java
// 1. Create an order
POST /api/orders
Response includes "self" link

// 2. Get the order
Follow "self" link from previous response
Response includes "process" and "cancel" links

// 3. Process the order
Follow "process" link with PATCH request
Response now includes "ship" and "cancel" links

// 4. Ship the order
Follow "ship" link with PATCH request
Response now includes only "deliver" link

// 5. Deliver the order
Follow "deliver" link with PATCH request
Response includes no action links (terminal state)
```

## Testing HATEOAS

### Manual Testing with curl

```bash
# Create an order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST123",
    "items": [
      {
        "productId": "PROD456",
        "quantity": 2,
        "unitPrice": 149.99
      }
    ]
  }'

# Get the order (extract ID from previous response)
curl http://localhost:8080/api/orders/{orderId}

# Follow the "process" link
curl -X PATCH http://localhost:8080/api/orders/{orderId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "PROCESSING"}'
```

### Automated Testing

Unit tests and integration tests should verify:
1. Presence of expected links in responses
2. Correctness of link URLs
3. State-based link availability
4. Pagination links in collection responses

## Best Practices

1. **Use Standard Link Relations**: Use standard IANA link relations when applicable (e.g., `self`, `next`, `prev`)
2. **Custom Relations**: Use descriptive names for custom link relations (e.g., `cancel`, `process`, `ship`)
3. **Consistent Structure**: Maintain consistent link structure across all responses
4. **Documentation**: Document link relations and their meanings
5. **Versioning**: Include API version in link relations if needed for backward compatibility

## Future Enhancements

1. **HAL-FORMS**: Add form templates for POST/PUT/PATCH operations
2. **Collection+JSON**: Support alternative hypermedia formats
3. **Link Templates**: Use URI templates for parameterized links
4. **Profile Links**: Add links to JSON Schema or documentation
5. **Caching Hints**: Include cache-control information in links

## References

- [Richardson Maturity Model](https://martinfowler.com/articles/richardsonMaturityModel.html)
- [Spring HATEOAS Documentation](https://spring.io/projects/spring-hateoas)
- [IANA Link Relations](https://www.iana.org/assignments/link-relations/link-relations.xhtml)
- [HAL Specification](http://stateless.co/hal_specification.html)

