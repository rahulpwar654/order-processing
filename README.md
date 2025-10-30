# Order Processing System

A robust e-commerce order processing backend system built with Spring Boot 3, implementing order lifecycle management, status transitions, and automated order processing.

## Features

- ✅ Create orders with multiple items
- ✅ Retrieve order details
- ✅ List orders with pagination and status filtering
- ✅ Update order status with enforced state transitions
- ✅ Cancel pending orders
- ✅ Automated background job (every 5 minutes) to promote PENDING → PROCESSING
- ✅ Comprehensive validation and error handling
- ✅ RESTful API design
- ✅ H2 in-memory database for development
- ✅ Full test coverage (51+ tests)
- ✅ **Redis caching** for high performance (70-80% DB load reduction)
- ✅ **Circuit breaker** protection against cascading failures
- ✅ **Rate limiting** to prevent system overload
- ✅ **Actuator endpoints** for monitoring and health checks
- ✅ **Database indexing** for optimized queries
- ✅ **N+1 query prevention** with entity graphs

## Technology Stack

- **Java 21**
- **Spring Boot 3.5.7**
- **Spring Data JPA** (Hibernate)
- **Spring Validation**
- **Spring Cache** (Redis)
- **Spring Actuator** (monitoring)
- **Resilience4j** (circuit breaker, rate limiting)
- **H2 Database** (in-memory)
- **Redis** (caching)
- **Lombok** (reduce boilerplate)
- **Maven** (build tool)
- **JUnit 5** (testing)
- **Mockito** (mocking)
- **MockMvc** (API testing)


## External Pods

- **Zipkin**
- **Redis 7.x** (for caching)
- 
## Project Structure

```
src/
├── main/
│   ├── java/com/example/order/
│   │   ├── OrderApplication.java           # Main application
│   │   ├── controller/
│   │   │   └── OrderController.java        # REST endpoints
│   │   ├── service/
│   │   │   ├── OrderService.java           # Service interface
│   │   │   ├── OrderMapper.java            # DTO mapper
│   │   │   └── impl/
│   │   │       └── OrderServiceImpl.java   # Business logic
│   │   ├── model/
│   │   │   ├── Order.java                  # Order entity
│   │   │   ├── OrderItem.java              # OrderItem entity
│   │   │   └── OrderStatus.java            # Status enum
│   │   ├── repository/
│   │   │   ├── OrderRepository.java        # Order data access
│   │   │   └── OrderItemRepository.java    # OrderItem data access
│   │   ├── dto/
│   │   │   ├── OrderCreateRequest.java     # Create order request
│   │   │   ├── OrderItemRequest.java       # Order item in request
│   │   │   ├── OrderStatusUpdateRequest.java # Status update request
│   │   │   └── OrderResponse.java          # Order response
│   │   ├── exception/
│   │   │   ├── NotFoundException.java      # 404 exception
│   │   │   ├── ConflictException.java      # 409 exception
│   │   │   ├── ApiError.java               # Error response model
│   │   │   └── GlobalExceptionHandler.java # Global error handler
│   │   └── scheduling/
│   │       └── OrderStatusScheduler.java   # Background job
│   └── resources/
│       └── application.yml                  # Configuration
└── test/
    └── java/com/example/order/
        ├── OrderApplicationTests.java       # Smoke test
        ├── controller/
        │   └── OrderControllerTest.java     # Controller unit tests
        ├── service/impl/
        │   └── OrderServiceImplTest.java    # Service unit tests
        ├── scheduling/
        │   └── OrderStatusSchedulerTest.java # Scheduler unit tests
        └── integration/
            └── OrderIntegrationTest.java    # Integration tests
```

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6+ (or use included Maven wrapper)

### Build the Project

```cmd
mvnw.cmd clean package
```

### Run the Application

```cmd
mvnw.cmd spring-boot:run
```

The application will start on **http://localhost:8080**

### Run Tests

```cmd
mvnw.cmd test
```

**Test Results**: 51 tests, 0 failures ✅

See [TEST_COVERAGE.md](doc/TEST_COVERAGE.md) for detailed test documentation.

### Access H2 Console (Development)

- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:ordersdb`
- Username: `sa`
- Password: _(leave empty)_

### Monitoring Endpoints

- **Health Check**: http://localhost:8080/actuator/health
- **Circuit Breakers**: http://localhost:8080/actuator/circuitbreakers
- **Rate Limiters**: http://localhost:8080/actuator/ratelimiters
- **Metrics**: http://localhost:8080/actuator/metrics
- **All Endpoints**: http://localhost:8080/actuator

## API Documentation

### Base URL
```
http://localhost:8080/api/orders
```

### Endpoints

#### 1. Create Order
**POST** `/api/orders`

**Request Body**:
```json
{
  "customerId": "11",
  "items": [
    {
      "productId": "Paper",
      "quantity": 50,
      "unitPrice": 5
    },{
      "productId": "ABC",
      "quantity": 10,
      "unitPrice": 40
    },{
      "productId": "XYZ",
      "quantity": 5,
      "unitPrice": 100
    }
  ]
}
```

**Response** (201 Created):
```json
{
  "id": "9c6be681-1694-4104-90ce-2f938eecea33",
  "customerId": "11",
  "status": "PROCESSING",
  "totalAmount": 1150,
  "items": [
    {
      "productId": "Paper",
      "quantity": 50,
      "unitPrice": 5,
      "lineTotal": 250
    },
    {
      "productId": "ABC",
      "quantity": 10,
      "unitPrice": 40,
      "lineTotal": 400
    },
    {
      "productId": "XYZ",
      "quantity": 5,
      "unitPrice": 100,
      "lineTotal": 500
    }
  ],
  "createdAt": "2025-10-30T14:34:12.716302Z",
  "updatedAt": "2025-10-30T14:34:12.716302Z",
  "canceledAt": null
}
```

#### 2. Get Order by ID
**GET** `/api/orders/{id}`

**Response** (200 OK):
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "customerId": "cust-123",
  "status": "PENDING",
  "totalAmount": 26.00,
  "items": [...],
  "createdAt": "2025-10-30T06:00:00Z",
  "updatedAt": "2025-10-30T06:00:00Z",
  "canceledAt": null
}
```

#### 3. List Orders
**GET** `/api/orders?status={STATUS}&page={page}&size={size}`

**Query Parameters**:
- `status` (optional): Filter by status (PENDING, PROCESSING, SHIPPED, DELIVERED)
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)

**Response** (200 OK):
```json
{
  "content": [...],
  "pageable": {...},
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

#### 4. Update Order Status
**PATCH** `/api/orders/{id}/status`

**Request Body**:
```json
{
  "status": "SHIPPED"
}
```

**Allowed Transitions**:
- PROCESSING → SHIPPED
- SHIPPED → DELIVERED

**Response** (200 OK):
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "SHIPPED",
  ...
}
```

#### 5. Cancel Order
**POST** `/api/orders/{id}/cancel`

**Response** (200 OK):
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "PENDING",
  "canceledAt": "2025-10-30T07:00:00Z",
  ...
}
```

**Note**: Only orders with status `PENDING` can be canceled.

### Error Responses

#### 400 Bad Request (Validation Error)
```json
{
  "timestamp": "2025-10-30T06:00:00Z",
  "path": "/api/orders",
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": [
    "customerId must not be blank",
    "quantity must be positive"
  ]
}
```

#### 404 Not Found
```json
{
  "timestamp": "2025-10-30T06:00:00Z",
  "path": "/api/orders/123",
  "code": "ORDER_NOT_FOUND",
  "message": "Order 123 not found"
}
```

#### 409 Conflict
```json
{
  "timestamp": "2025-10-30T06:00:00Z",
  "path": "/api/orders/123/cancel",
  "code": "CONFLICT",
  "message": "Can only cancel orders in PENDING status"
}
```

## Business Rules

### Order Status Lifecycle

```
PENDING → PROCESSING → SHIPPED → DELIVERED
         ↑
    (scheduled)
```

1. **PENDING**: Initial state when order is created
2. **PROCESSING**: Automatically set by scheduler every 5 minutes (non-canceled orders only)
3. **SHIPPED**: Manually updated from PROCESSING
4. **DELIVERED**: Manually updated from SHIPPED

### Status Transition Rules

| From | To | Allowed | Method |
|------|----|---------| -------|
| PENDING | PROCESSING | ✅ | Scheduler (automatic) |
| PENDING | Other | ❌ | Manual updates blocked |
| PROCESSING | SHIPPED | ✅ | Manual |
| PROCESSING | Other | ❌ | Invalid transition |
| SHIPPED | DELIVERED | ✅ | Manual |
| SHIPPED | Other | ❌ | Invalid transition |
| DELIVERED | Any | ❌ | No further updates |

### Cancellation Rules

- ✅ Can cancel: Orders with status `PENDING` and not already canceled
- ❌ Cannot cancel: Orders in `PROCESSING`, `SHIPPED`, or `DELIVERED` status
- ❌ Cannot cancel: Orders already canceled (idempotency)

### Validation Rules

- **customerId**: Required, not blank
- **items**: Required, not empty
- **quantity**: Required, must be positive (> 0)
- **unitPrice**: Required, must be >= 0
- **totalAmount**: Calculated automatically (sum of all line totals)
- **lineTotal**: Calculated automatically (quantity × unitPrice)

## Scheduler

**Job**: `OrderStatusScheduler.promotePendingToProcessing()`

**Schedule**: Every 5 minutes (cron: `0 */5 * * * *`)

**Behavior**:
- Finds all orders with status `PENDING` and `canceledAt` is `null`
- Updates their status to `PROCESSING` in bulk
- Logs the number of orders updated

## Configuration

See `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: order
  datasource:
    url: jdbc:h2:mem:ordersdb
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  h2:
    console:
      enabled: true
      path: /h2-console
```

## License

This project is created for educational/demonstration purposes.

