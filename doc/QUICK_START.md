# Quick Start Guide - Order Processing API

## Issue Fixed ✅

**Problem:** Circuit breaker triggering on POST `/api/orders` with Jackson serialization error for `java.time.Instant`

**Status:** RESOLVED

## What Was Fixed

1. ✅ Jackson JSR310 module configured for Java 8 date/time support
2. ✅ Proper HTTP status codes (503 for service unavailable, 429 for rate limiting)
3. ✅ Resilient cache error handling
4. ✅ Swagger/OpenAPI documentation integrated

## Start the Application

```bash
cd D:\java\order-processing-java
mvn spring-boot:run
```

## Test the Fix

### 1. Create an Order (should return 201 Created)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d "{\"customerId\":\"CUST123\",\"items\":[{\"productId\":\"PROD456\",\"quantity\":2,\"unitPrice\":149.99}]}"
```

**Expected Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "CUST123",
  "status": "PENDING",
  "totalAmount": 299.98,
  "createdAt": "2025-10-30T18:43:45.123Z",
  "updatedAt": "2025-10-30T18:43:45.123Z",
  "canceledAt": null,
  "_links": {
    "self": {"href": "http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000"},
    "orders": {"href": "http://localhost:8080/api/orders?page=0&size=20"},
    "customer-orders": {"href": "http://localhost:8080/api/orders/customer/CUST123?page=0&size=20"},
    "process": {"href": "http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000/status"},
    "cancel": {"href": "http://localhost:8080/api/orders/550e8400-e29b-41d4-a716-446655440000/cancel"}
  }
}
```

### 2. Check Circuit Breaker Status

```bash
curl http://localhost:8080/actuator/circuitbreakers
```

**Expected:** `"state": "CLOSED"`

### 3. View API Documentation

Open in browser: http://localhost:8080/swagger-ui.html

## Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Create new order |
| GET | `/api/orders/{id}` | Get order by ID |
| GET | `/api/orders` | List all orders (paginated) |
| GET | `/api/orders/customer/{customerId}` | Get customer's orders |
| PATCH | `/api/orders/{id}/status` | Update order status |
| POST | `/api/orders/{id}/cancel` | Cancel order |

## Monitoring Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Application health |
| `/actuator/circuitbreakers` | Circuit breaker status |
| `/actuator/ratelimiters` | Rate limiter status |
| `/actuator/metrics` | Application metrics |
| `/actuator/prometheus` | Prometheus metrics |
| `/swagger-ui.html` | API documentation |
| `/v3/api-docs` | OpenAPI JSON spec |

## Error Codes You May See

| HTTP | Code | Meaning |
|------|------|---------|
| 201 | - | Order created successfully |
| 200 | - | Request successful |
| 400 | VALIDATION_ERROR | Invalid input data |
| 404 | ORDER_NOT_FOUND | Order doesn't exist |
| 409 | CONFLICT | Business logic conflict (e.g., can't cancel shipped order) |
| 429 | TOO_MANY_REQUESTS | Rate limit exceeded (20 req/sec for creates) |
| 503 | SERVICE_UNAVAILABLE | Service temporarily down (circuit breaker open) |

## If You Still See Issues

### Redis Not Running (Optional)

Redis cache is optional. If not running, you'll see logged warnings but service will work:

```yaml
# application.yml - disable cache
spring:
  cache:
    type: none
```

### Check Logs

```bash
tail -f logs/application.log
```

Look for:
- ✅ No "Circuit breaker triggered" messages
- ✅ No Jackson serialization errors
- ⚠️ Cache warnings are OK (service degrades gracefully)

### Verify Dependencies

```bash
mvn dependency:tree | grep jackson-datatype-jsr310
```

Should show: `com.fasterxml.jackson.datatype:jackson-datatype-jsr310`

## Configuration Files

All resilience settings are in:
- `src/main/resources/application.yml`

Key settings:
- Circuit breaker: 50% failure threshold, 5 min calls, 60s open duration
- Rate limiting: 20 req/sec for creates, 200 req/sec for queries
- Cache TTL: 15min (orders), 5min (lists), 10min (customer orders)

## Documentation

- [CIRCUIT_BREAKER_RESOLUTION.md](./CIRCUIT_BREAKER_RESOLUTION.md) - Complete issue resolution
- [JACKSON_JSR310_FIX.md](./JACKSON_JSR310_FIX.md) - Jackson configuration details
- [HATEOAS_IMPLEMENTATION.md](./HATEOAS_IMPLEMENTATION.md) - Hypermedia links
- [SWAGGER_OPENAPI.md](./SWAGGER_OPENAPI.md) - API documentation

## Support

If issues persist:
1. Check actuator health endpoints
2. Review application logs
3. Verify Redis is running (or disabled)
4. Ensure Java 21 is installed
5. Check port 8080 is available

## Success Criteria ✅

- [x] POST /api/orders returns 201 with ISO-8601 dates
- [x] Circuit breaker status is CLOSED
- [x] No Jackson serialization errors in logs
- [x] HATEOAS links present in responses
- [x] Swagger UI accessible
- [x] Cache failures don't break service
- [x] Proper HTTP status codes (503, 429, etc.)

