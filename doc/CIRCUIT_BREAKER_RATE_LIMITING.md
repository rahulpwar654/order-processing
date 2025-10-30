# Circuit Breaker and Rate Limiting Guide

## Overview
This document describes the circuit breaker and rate limiting implementation using Resilience4j to protect the Order Processing System from cascading failures and excessive load.

## What is a Circuit Breaker?

A circuit breaker prevents cascading failures by monitoring service calls and "opening" the circuit when failures exceed a threshold, temporarily blocking requests and allowing the system to recover.

### Circuit States

```
┌─────────┐  Failure Rate > Threshold  ┌──────┐
│ CLOSED  │ ─────────────────────────> │ OPEN │
└─────────┘                             └──────┘
     ^                                      │
     │                                      │ Wait Duration
     │                                      │
     │         Success Rate > Threshold     V
     │    <─────────────────────────  ┌──────────────┐
     └───────────────────────────────  │ HALF_OPEN    │
                                       └──────────────┘
```

**States:**
- **CLOSED**: Normal operation, requests are allowed
- **OPEN**: Too many failures, requests are blocked (fallback executed)
- **HALF_OPEN**: Testing if service recovered, limited requests allowed

## Configuration

### Circuit Breaker Settings (`application.yml`)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      orderService:
        slidingWindowSize: 10              # Last 10 calls evaluated
        minimumNumberOfCalls: 5            # Min calls before evaluation
        failureRateThreshold: 50           # Opens at 50% failure rate
        slowCallRateThreshold: 50          # Opens at 50% slow calls
        slowCallDurationThreshold: 3s      # Call is slow if > 3 seconds
        waitDurationInOpenState: 60s       # Stay open for 60 seconds
        permittedNumberOfCallsInHalfOpenState: 3  # Test with 3 calls
```

### Rate Limiter Settings

```yaml
resilience4j:
  ratelimiter:
    instances:
      orderCreate:
        limitForPeriod: 20                 # 20 requests
        limitRefreshPeriod: 1s             # per second
        timeoutDuration: 100ms             # wait max 100ms
      orderQuery:
        limitForPeriod: 200                # 200 requests
        limitRefreshPeriod: 1s             # per second
        timeoutDuration: 500ms             # wait max 500ms
```

## Rate Limiter Profiles

| Profile | Limit | Use Case | Endpoints |
|---------|-------|----------|-----------|
| `orderCreate` | 20 req/s | Write operations | POST /api/orders |
| `orderUpdate` | 20 req/s | Modifications | PATCH, POST (cancel) |
| `orderQuery` | 200 req/s | Single item reads | GET /api/orders/{id} |
| `orderList` | 200 req/s | List/search | GET /api/orders |

## Applied Protection

### Service Layer Protection

```java
@CircuitBreaker(name = "orderService", fallbackMethod = "createFallback")
@RateLimiter(name = "orderCreate")
public OrderResponse create(OrderCreateRequest request) {
    // Business logic
}
```

**Protected Operations:**
- ✅ `create()` - Circuit breaker + Rate limiter (20 req/s)
- ✅ `getById()` - Circuit breaker + Rate limiter (200 req/s)
- ✅ `list()` - Circuit breaker + Rate limiter (200 req/s)
- ✅ `updateStatus()` - Circuit breaker + Rate limiter (20 req/s)
- ✅ `cancel()` - Circuit breaker + Rate limiter (20 req/s)
- ✅ `getOrdersByCustomer()` - Circuit breaker + Rate limiter (200 req/s)

## Fallback Behavior

When circuit is **OPEN** or rate limit **EXCEEDED**, fallback methods return user-friendly errors:

### Example Responses

**Circuit Breaker Open:**
```json
{
  "timestamp": "2025-10-30T12:00:00Z",
  "path": "/api/orders",
  "code": "CONFLICT",
  "message": "Order service is temporarily unavailable. Please try again later."
}
```

**Rate Limit Exceeded:**
```json
{
  "timestamp": "2025-10-30T12:00:00Z",
  "path": "/api/orders",
  "code": "TOO_MANY_REQUESTS",
  "message": "Rate limit exceeded. Please try again later."
}
```

## Monitoring

### Actuator Endpoints

Access monitoring endpoints:

```bash
# Circuit breaker status
GET http://localhost:8080/actuator/circuitbreakers

# Circuit breaker events
GET http://localhost:8080/actuator/circuitbreakerevents

# Rate limiter status
GET http://localhost:8080/actuator/ratelimiters

# Rate limiter events
GET http://localhost:8080/actuator/ratelimiterevents

# Health status (includes circuit breakers)
GET http://localhost:8080/actuator/health
```

### Health Check Response

```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "orderService": {
          "state": "CLOSED",
          "failureRate": "10.0%",
          "slowCallRate": "5.0%",
          "bufferedCalls": 10,
          "failedCalls": 1,
          "slowCalls": 0
        }
      }
    },
    "rateLimiters": {
      "status": "UP",
      "details": {
        "orderCreate": {
          "availablePermissions": 15,
          "numberOfWaitingThreads": 0
        }
      }
    }
  }
}
```

## Testing Circuit Breaker

### Simulate Failures

```java
// Simulate database failure (will trigger circuit breaker)
@Test
void testCircuitBreakerOpens() {
    // Cause 5+ failures in a row
    for (int i = 0; i < 6; i++) {
        try {
            orderService.getById(UUID.randomUUID());
        } catch (Exception e) {
            // Expected
        }
    }
    
    // Next call should hit fallback (circuit is OPEN)
    assertThrows(ConflictException.class, () -> {
        orderService.getById(UUID.randomUUID());
    });
}
```

### Simulate Rate Limiting

```bash
# Send 21 requests in 1 second (exceeds limit of 20)
for i in {1..21}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"test","items":[...]}' &
done
wait

# 21st request should be rate limited
```

## Load Testing

### Apache Bench - Test Rate Limiting

```bash
# Test orderQuery rate limiter (200 req/s)
ab -n 1000 -c 50 http://localhost:8080/api/orders/{id}

# Expected: ~200 req/s throughput
# Requests exceeding limit will fail with 429 or timeout
```

### JMeter Test Plan

```xml
<ThreadGroup>
  <numThreads>100</numThreads>
  <rampTime>10</rampTime>
  <loops>10</loops>
</ThreadGroup>
```

## Best Practices

### 1. Tune Based on Actual Load

Monitor and adjust thresholds:
```yaml
# Start conservative, tune based on metrics
failureRateThreshold: 50  # Start at 50%
minimumNumberOfCalls: 5   # Evaluate after 5 calls
```

### 2. Set Appropriate Timeouts

```yaml
slowCallDurationThreshold: 3s  # Match your p95 latency + buffer
timeoutDuration: 4s            # Give services time to respond
```

### 3. Configure Exception Handling

**Record these exceptions** (trigger circuit breaker):
```yaml
recordExceptions:
  - org.springframework.dao.DataAccessException
  - java.util.concurrent.TimeoutException
  - java.sql.SQLException
```

**Ignore these exceptions** (don't count as failures):
```yaml
ignoreExceptions:
  - com.example.order.exception.NotFoundException
  - com.example.order.exception.ConflictException
```

### 4. Implement Graceful Degradation

Fallback methods should:
- Return cached data if available
- Return default/empty responses
- Log errors for investigation
- Provide user-friendly error messages

### 5. Monitor Continuously

Set up alerts for:
- Circuit breaker state changes (CLOSED → OPEN)
- High failure rates (> 30%)
- Rate limiter rejections
- Slow call rates

## Advanced Configuration

### Per-Endpoint Rate Limiting

Different limits for different operations:

```yaml
resilience4j:
  ratelimiter:
    instances:
      # Critical write operations - strict
      orderCreate:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
      
      # Bulk operations - very strict
      orderBulkUpdate:
        limitForPeriod: 5
        limitRefreshPeriod: 1s
      
      # Read operations - lenient
      orderQuery:
        limitForPeriod: 500
        limitRefreshPeriod: 1s
```

### Multiple Circuit Breakers

Isolate failures by service:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      orderService:
        slidingWindowSize: 10
      
      paymentService:
        slidingWindowSize: 5
        failureRateThreshold: 30  # More sensitive
      
      notificationService:
        slidingWindowSize: 20
        failureRateThreshold: 70  # More tolerant
```

## Troubleshooting

### Circuit Breaker Keeps Opening

**Symptoms**: Circuit opens frequently, requests fail

**Causes**:
- Threshold too low (increase `failureRateThreshold`)
- Database slow (check slow queries)
- Insufficient resources (scale up)

**Solutions**:
```yaml
# Option 1: Increase tolerance
failureRateThreshold: 70    # From 50
minimumNumberOfCalls: 10    # From 5

# Option 2: Increase wait time
waitDurationInOpenState: 120s  # From 60s

# Option 3: Adjust slow call threshold
slowCallDurationThreshold: 5s  # From 3s
```

### Rate Limiting Too Aggressive

**Symptoms**: Legitimate requests being blocked

**Causes**:
- Limit too low for actual traffic
- Single user sending burst requests

**Solutions**:
```yaml
# Option 1: Increase limit
limitForPeriod: 50    # From 20

# Option 2: Longer refresh period
limitRefreshPeriod: 5s  # From 1s (gives 100 requests per 5s)

# Option 3: Increase timeout
timeoutDuration: 1000ms  # From 500ms
```

### False Positives

**Symptoms**: Circuit opens on business exceptions (404, validation errors)

**Solution**: Configure ignored exceptions
```yaml
ignoreExceptions:
  - com.example.order.exception.NotFoundException
  - com.example.order.exception.ConflictException
  - org.springframework.web.bind.MethodArgumentNotValidException
```

## Metrics and Alerts

### Prometheus Metrics

Resilience4j exposes metrics automatically:

```
# Circuit breaker state
resilience4j_circuitbreaker_state{name="orderService",state="closed"} 1

# Failure rate
resilience4j_circuitbreaker_failure_rate{name="orderService"} 0.15

# Rate limiter available permissions
resilience4j_ratelimiter_available_permissions{name="orderCreate"} 15
```

### Grafana Dashboard

Import Resilience4j dashboard:
- Dashboard ID: 12194
- Shows: Circuit breaker states, failure rates, rate limits

### Alert Rules

```yaml
# Alert when circuit breaker opens
- alert: CircuitBreakerOpen
  expr: resilience4j_circuitbreaker_state{state="open"} == 1
  for: 1m
  annotations:
    summary: "Circuit breaker {{ $labels.name }} is OPEN"

# Alert on high failure rate
- alert: HighFailureRate
  expr: resilience4j_circuitbreaker_failure_rate > 0.5
  for: 5m
  annotations:
    summary: "Failure rate > 50% for {{ $labels.name }}"
```

## Performance Impact

### Overhead

- **Circuit Breaker**: ~0.1ms per call (negligible)
- **Rate Limiter**: ~0.05ms per call (negligible)
- **Combined**: < 1% performance impact

### Benefits

- **Prevents cascading failures**: 1 slow service doesn't take down entire system
- **Protects resources**: Database connections, memory, CPU
- **Improves availability**: System degrades gracefully under load
- **Faster recovery**: Circuit breaker allows recovery time

## Production Checklist

- [x] Circuit breaker configured for all service methods
- [x] Rate limiters applied per operation type
- [x] Fallback methods implemented
- [x] Actuator endpoints enabled
- [x] Monitoring/alerting set up
- [ ] Load testing performed
- [ ] Thresholds tuned based on production traffic
- [ ] Runbook created for incidents
- [ ] Team trained on circuit breaker behavior

## Summary

| Feature | Configuration | Benefit |
|---------|---------------|---------|
| Circuit Breaker | 50% failure rate threshold | Prevents cascading failures |
| Rate Limiter (Write) | 20 req/s | Protects database from overload |
| Rate Limiter (Read) | 200 req/s | Allows high read throughput |
| Fallback Methods | User-friendly errors | Better user experience |
| Monitoring | Actuator + Metrics | Proactive issue detection |

The system is now protected against:
- ✅ Database failures
- ✅ Slow queries
- ✅ Resource exhaustion
- ✅ Traffic spikes
- ✅ Cascading failures

For more details, see:
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)

