# Idempotency Implementation for Order Creation

## Overview

Idempotency ensures that duplicate order creation requests (due to network retries, user double-clicks, etc.) don't result in multiple orders being created. The same request made multiple times will return the same order.

## Implementation

### How It Works

1. **Idempotency Key**: A unique identifier for each order creation request
2. **Duplicate Detection**: Before creating an order, the system checks if an order with the same idempotency key already exists
3. **Return Existing**: If found, returns the existing order instead of creating a new one

### Three Ways to Provide Idempotency Key

#### 1. Via HTTP Header (Recommended - Industry Standard)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
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
```

**Used by:** Stripe, PayPal, Square, and other major APIs

#### 2. Via Request Body
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST123",
    "idempotencyKey": "order-2025-10-30-001",
    "items": [
      {
        "productId": "PROD456",
        "quantity": 2,
        "unitPrice": 149.99
      }
    ]
  }'
```

#### 3. Auto-Generated (Automatic)
If no idempotency key is provided, the system automatically generates one based on the request content using SHA-256 hash:

```bash
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
```

The system generates: `a3f2c1b9d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1`

**Same request → Same hash → Same order**

## Priority Order

When multiple methods are used, the priority is:

1. **HTTP Header** (`Idempotency-Key`)
2. **Request Body** (`idempotencyKey` field)
3. **Auto-generated** (SHA-256 hash of request content)

## Database Schema

The `Order` entity includes:

```java
@Column(unique = true, length = 255)
private String idempotencyKey;
```

With index for fast lookup:

```java
@Index(name = "idx_order_idempotency_key", columnList = "idempotencyKey")
```

## Use Cases

### 1. Network Retry
```
Client sends request → Network timeout
Client retries → Same idempotency key
Server returns existing order (no duplicate created)
```

### 2. User Double-Click
```
User clicks "Place Order"
Request 1 sent with key "user-session-12345"
User accidentally clicks again
Request 2 sent with same key "user-session-12345"
Server returns the same order from Request 1
```

### 3. Mobile App Offline Queue
```
Mobile app queues order while offline
App comes online, sends order
Connection drops, app retries
Same order sent multiple times
All requests use same idempotency key → Only one order created
```

## Example Scenarios

### Scenario 1: First Request (Creates Order)

**Request:**
```bash
POST /api/orders
Idempotency-Key: my-unique-key-123

{
  "customerId": "CUST123",
  "items": [{"productId": "PROD456", "quantity": 2, "unitPrice": 149.99}]
}
```

**Response: 201 Created**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST123",
  "status": "PENDING",
  "totalAmount": 299.98,
  "createdAt": "2025-10-30T18:43:45.123Z",
  ...
}
```

### Scenario 2: Duplicate Request (Returns Existing)

**Request:**
```bash
POST /api/orders
Idempotency-Key: my-unique-key-123

{
  "customerId": "CUST123",
  "items": [{"productId": "PROD456", "quantity": 2, "unitPrice": 149.99}]
}
```

**Response: 201 Created** (same order, not a new one)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",  // SAME ORDER ID
  "customerId": "CUST123",
  "status": "PENDING",
  "totalAmount": 299.98,
  "createdAt": "2025-10-30T18:43:45.123Z",  // Original creation time
  ...
}
```

### Scenario 3: Different Content, Same Key (Not Recommended)

**First Request:**
```bash
POST /api/orders
Idempotency-Key: duplicate-key

{"customerId": "CUST123", "items": [{"productId": "PROD1", "quantity": 1, "unitPrice": 100}]}
```

**Second Request (different content):**
```bash
POST /api/orders
Idempotency-Key: duplicate-key

{"customerId": "CUST123", "items": [{"productId": "PROD2", "quantity": 2, "unitPrice": 200}]}
```

**Result:** Returns the FIRST order (with PROD1), ignoring the new content.

⚠️ **Best Practice:** Use unique keys for genuinely different orders.

## Code Implementation

### OrderCreateRequest
```java
public class OrderCreateRequest {
    private String customerId;
    private List<OrderItemRequest> items;
    private String idempotencyKey;  // NEW FIELD
}
```

### Order Entity
```java
@Column(unique = true, length = 255)
private String idempotencyKey;
```

### OrderRepository
```java
Optional<Order> findByIdempotencyKey(String idempotencyKey);
```

### OrderServiceImpl
```java
public OrderResponse create(OrderCreateRequest request) {
    // Generate or use provided key
    String idempotencyKey = request.getIdempotencyKey();
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
        idempotencyKey = IdempotencyKeyGenerator.generateKey(request);
    }
    
    // Check for duplicate
    Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
    if (existing.isPresent()) {
        return OrderMapper.toResponse(existing.get());
    }
    
    // Create new order
    Order order = Order.builder()
        .idempotencyKey(idempotencyKey)
        // ...
        .build();
    // ...
}
```

### OrderController
```java
@PostMapping
public OrderResponse create(@RequestBody OrderCreateRequest request,
                           @RequestHeader(value = "Idempotency-Key", required = false) String header) {
    if (header != null && !header.isBlank()) {
        request.setIdempotencyKey(header);
    }
    return orderService.create(request);
}
```

## Best Practices

### For Clients

1. **Generate Unique Keys**: Use UUIDs, timestamps, or user-session identifiers
   ```javascript
   const idempotencyKey = `${userId}-${timestamp}-${uuid()}`;
   ```

2. **Store Keys**: Save the key with the request so retries use the same key
   ```javascript
   localStorage.setItem('orderKey', idempotencyKey);
   ```

3. **Retry with Same Key**: On failure, retry with the same key
   ```javascript
   async function createOrder(data) {
     let key = localStorage.getItem('orderKey');
     if (!key) {
       key = generateKey();
       localStorage.setItem('orderKey', key);
     }
     
     const response = await fetch('/api/orders', {
       method: 'POST',
       headers: {
         'Content-Type': 'application/json',
         'Idempotency-Key': key
       },
       body: JSON.stringify(data)
     });
     
     if (response.ok) {
       localStorage.removeItem('orderKey');  // Clear after success
     }
     
     return response;
   }
   ```

4. **Clear Keys After Success**: Remove the key after successful order creation

### For API

1. **Return Same Status Code**: Always return 201 for both new and existing orders
2. **Include Creation Time**: Clients can check `createdAt` to detect duplicates
3. **Log Duplicate Attempts**: Monitor for potential issues
4. **Consider TTL**: Optionally expire idempotency keys after 24-48 hours

## Testing

### Test 1: Duplicate Detection
```bash
# First request
curl -X POST http://localhost:8080/api/orders \
  -H "Idempotency-Key: test-key-001" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST123","items":[{"productId":"PROD456","quantity":2,"unitPrice":149.99}]}'

# Note the order ID in response

# Second request (duplicate)
curl -X POST http://localhost:8080/api/orders \
  -H "Idempotency-Key: test-key-001" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST123","items":[{"productId":"PROD456","quantity":2,"unitPrice":149.99}]}'

# Should return SAME order ID
```

### Test 2: Auto-Generated Key
```bash
# Request 1
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST999","items":[{"productId":"PROD111","quantity":1,"unitPrice":99.99}]}'

# Request 2 (identical content)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST999","items":[{"productId":"PROD111","quantity":1,"unitPrice":99.99}]}'

# Should return SAME order (auto-generated key matches)
```

### Test 3: Different Keys = Different Orders
```bash
# Request 1
curl -X POST http://localhost:8080/api/orders \
  -H "Idempotency-Key: key-A" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST123","items":[{"productId":"PROD456","quantity":2,"unitPrice":149.99}]}'

# Request 2 (different key)
curl -X POST http://localhost:8080/api/orders \
  -H "Idempotency-Key: key-B" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST123","items":[{"productId":"PROD456","quantity":2,"unitPrice":149.99}]}'

# Should return DIFFERENT order IDs
```

## Monitoring

### Metrics to Track

1. **Duplicate Rate**: Percentage of requests that hit existing orders
   ```java
   @Observed(name = "order.create.duplicate")
   ```

2. **Idempotency Key Sources**: Header vs Body vs Auto-generated
   ```java
   log.info("Idempotency key source: {}", source);
   ```

3. **Failed Key Generation**: Auto-generation failures
   ```java
   log.error("Failed to generate idempotency key");
   ```

### Logs

Check logs for:
```
"Order already exists with idempotency key: {key}, returning existing order: {id}"
"Generated idempotency key: {key}"
"Using provided idempotency key: {key}"
```

## Limitations

1. **Key Uniqueness**: Keys are globally unique, not per-customer
   - Consider prefixing with customer ID if needed: `CUST123-key-001`

2. **No Expiration**: Currently, idempotency keys never expire
   - Future enhancement: Add TTL or cleanup job

3. **Content Changes**: Different content with same key returns first order
   - Clients should use unique keys for genuinely different orders

4. **Database Constraint**: Unique constraint prevents concurrent duplicates
   - Race conditions handled at database level

## Future Enhancements

1. **Key Expiration**: Expire keys after 24-48 hours
   ```java
   @Column
   private Instant idempotencyKeyExpiresAt;
   ```

2. **Conflict Detection**: Validate that duplicate requests have identical content
   ```java
   if (!requestMatches(existing, request)) {
       throw new ConflictException("Different content for same idempotency key");
   }
   ```

3. **Async Processing**: Support long-running order creation
   ```java
   if (existing.getStatus() == PROCESSING) {
       return 202 Accepted;
   }
   ```

4. **Metrics Dashboard**: Grafana/Prometheus tracking of duplicate rates

## References

- [Stripe Idempotency](https://stripe.com/docs/api/idempotent_requests)
- [RFC 5789 - PATCH Method](https://tools.ietf.org/html/rfc5789)
- [API Idempotency Best Practices](https://restfulapi.net/idempotent-rest-apis/)

## Related Files

- `src/main/java/com/example/order/dto/OrderCreateRequest.java`
- `src/main/java/com/example/order/model/Order.java`
- `src/main/java/com/example/order/util/IdempotencyKeyGenerator.java`
- `src/main/java/com/example/order/repository/OrderRepository.java`
- `src/main/java/com/example/order/service/impl/OrderServiceImpl.java`
- `src/main/java/com/example/order/controller/OrderController.java`

