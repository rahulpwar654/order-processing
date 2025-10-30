# Distributed Tracing and Logging Guide

## Overview

This document describes the distributed tracing and logging implementation using Micrometer Tracing, Zipkin, and custom filters for comprehensive request observability.

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐     ┌─────────┐
│   Client    │────>│  API Gateway │────>│ Application │────>│   DB    │
└─────────────┘     └──────────────┘     └─────────────┘     └─────────┘
       │                    │                     │                │
       │                    │                     │                │
       └────────────────────┴─────────────────────┴────────────────┘
                                    │
                                    ▼
                            ┌──────────────┐
                            │    Zipkin    │
                            │   (Tracing)  │
                            └──────────────┘
```

## Components

### 1. Micrometer Tracing
- **Purpose**: Distributed tracing across services
- **Implementation**: Brave (Zipkin-compatible)
- **Sampling**: 100% (configurable)

### 2. Zipkin
- **Purpose**: Trace collection and visualization
- **Endpoint**: http://localhost:9411
- **Storage**: In-memory (default)

### 3. Custom Filters
- **RequestTracingFilter**: Adds request ID and trace context
- **RequestResponseLoggingFilter**: Logs detailed request/response

### 4. Structured Logging
- **MDC**: Mapped Diagnostic Context with trace IDs
- **Pattern**: Includes traceId, spanId, requestId

## Request Flow

### 1. Request Arrives
```
HTTP Request → RequestTracingFilter
```

**Actions**:
- Generate/extract request ID
- Extract trace context from headers
- Add to MDC (traceId, spanId, requestId)
- Add request ID to response header
- Log request start

### 2. Request Processing
```
Controller → Service (with @Observed) → Repository
```

**Actions**:
- Create spans for each layer
- Add custom tags (order ID, customer ID, etc.)
- Log key events
- Measure execution time

### 3. Response Returned
```
Service → Controller → RequestTracingFilter → Client
```

**Actions**:
- Log response status
- Calculate duration
- Add trace headers to response
- Send trace to Zipkin
- Clean up MDC

## Configuration

### application.yml

```yaml
# Tracing Configuration
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling (adjust for production)

tracing:
  zipkin:
    endpoint: http://localhost:9411/api/v2/spans
    enabled: true

# Logging with Trace Context
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}] [%X{requestId:-}]"
```

### Production Sampling

```yaml
# Sample 10% of requests
management:
  tracing:
    sampling:
      probability: 0.1
```

## Custom Filters

### RequestTracingFilter

**Order**: 1 (executes first)

**Features**:
- Generates unique request ID
- Extracts trace context
- Adds to MDC for logging
- Adds headers to response
- Logs request start/end
- Calculates request duration

**Headers Added**:
- `X-Request-ID`: Unique request identifier
- `X-Trace-ID`: Distributed trace ID
- `X-Span-ID`: Current span ID

### RequestResponseLoggingFilter

**Order**: 2 (executes after tracing)

**Features**:
- Logs request method, URI, headers
- Logs request body (POST/PUT/PATCH)
- Logs response status, headers
- Logs response body
- Truncates large payloads
- Filters sensitive headers

**Excluded**:
- Actuator endpoints (no logging overhead)
- Sensitive headers (authorization, tokens, etc.)

## Instrumentation

### Service Layer

All service methods are instrumented with `@Observed`:

```java
@Observed(name = "order.create", contextualName = "creating-order")
public OrderResponse create(OrderCreateRequest request) {
    log.info("Creating order for customer: {}", request.getCustomerId());
    
    // Business logic
    
    // Add custom tags
    if (tracer.currentSpan() != null) {
        tracer.currentSpan().tag("order.id", saved.getId().toString());
        tracer.currentSpan().tag("order.customerId", saved.getCustomerId());
    }
    
    log.info("Order created successfully: {}", saved.getId());
    return response;
}
```

### Custom Tags

Each operation adds relevant business tags:

**create()**:
- `order.id`
- `order.customerId`
- `order.itemCount`
- `order.totalAmount`

**getById()**:
- `order.id`
- `order.status`

**list()**:
- `order.status`
- `page.number`
- `page.size`
- `result.totalElements`

**updateStatus()**:
- `order.id`
- `order.oldStatus`
- `order.newStatus`

**cancel()**:
- `order.id`
- `order.status`

## Log Format

### Log Entry Example

```
2025-10-30 12:00:00 - INFO [order,64f7c7f8a9b3c1e2,a1b2c3d4e5f6g7h8] [550e8400-e29b-41d4-a716-446655440000]
Request started: POST /api/orders from 192.168.1.100

2025-10-30 12:00:00 - DEBUG [order,64f7c7f8a9b3c1e2,a1b2c3d4e5f6g7h8] [550e8400-e29b-41d4-a716-446655440000]
Creating order for customer: cust-123

2025-10-30 12:00:00 - INFO [order,64f7c7f8a9b3c1e2,a1b2c3d4e5f6g7h8] [550e8400-e29b-41d4-a716-446655440000]
Order created successfully: f47ac10b-58cc-4372-a567-0e02b2c3d479

2025-10-30 12:00:00 - INFO [order,64f7c7f8a9b3c1e2,a1b2c3d4e5f6g7h8] [550e8400-e29b-41d4-a716-446655440000]
Request completed: POST /api/orders - Status: 201 - Duration: 45ms
```

**Format Breakdown**:
- `2025-10-30 12:00:00` - Timestamp
- `INFO` - Log level
- `[order,...]` - [application, traceId, spanId]
- `[550e8400...]` - [requestId]
- Message

## Zipkin Setup

### Run Zipkin (Docker)

```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```

### Access Zipkin UI

Open browser: http://localhost:9411

### Query Traces

1. **Find Traces**: Search by service, operation, duration
2. **View Trace**: See complete request flow
3. **Analyze Spans**: Identify slow operations
4. **Dependencies**: Visualize service dependencies

## Trace Examples

### Successful Order Creation

```
Trace ID: 64f7c7f8a9b3c1e2
Total Duration: 45ms

Spans:
├─ HTTP POST /api/orders (45ms)
│  ├─ order.create (42ms)
│  │  ├─ INSERT orders (15ms)
│  │  └─ INSERT order_items (8ms)
│  └─ Cache put (3ms)
```

### Order Retrieval (Cache Hit)

```
Trace ID: a1b2c3d4e5f6g7h8
Total Duration: 2ms

Spans:
├─ HTTP GET /api/orders/{id} (2ms)
│  └─ order.getById (1ms)
│     └─ Cache get (1ms) [HIT]
```

### Order Retrieval (Cache Miss)

```
Trace ID: 1a2b3c4d5e6f7g8h
Total Duration: 15ms

Spans:
├─ HTTP GET /api/orders/{id} (15ms)
│  └─ order.getById (14ms)
│     ├─ Cache get (1ms) [MISS]
│     ├─ SELECT orders (8ms)
│     └─ Cache put (2ms)
```

## Debugging with Traces

### Find Slow Requests

1. Open Zipkin UI
2. Filter by: `minDuration=1000` (> 1 second)
3. Analyze slow spans
4. Identify bottlenecks

### Trace Errors

1. Filter by: `error=true`
2. View error tags and logs
3. See complete context
4. Root cause analysis

### Trace Specific Customer

1. Search by tag: `order.customerId=cust-123`
2. View all customer operations
3. Analyze patterns
4. Debug issues

## Custom Logging

### Controller Layer

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    @PostMapping
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest request) {
        log.info("API: Creating order for customer: {}", request.getCustomerId());
        OrderResponse response = orderService.create(request);
        log.info("API: Order created: {}", response.getId());
        return response;
    }
}
```

### Service Layer (Already Implemented)

```java
@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    
    @Observed(name = "order.create")
    public OrderResponse create(OrderCreateRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());
        // Business logic
        log.info("Order created successfully: {}", saved.getId());
        return response;
    }
}
```

## Performance Impact

| Component | Overhead | Notes |
|-----------|----------|-------|
| Tracing (100% sampling) | ~1-2ms per request | Synchronous |
| Tracing (10% sampling) | ~0.1-0.2ms per request | Recommended for prod |
| Request Filter | ~0.5ms | MDC operations |
| Logging Filter | ~1-2ms | Debug level only |
| Zipkin Export | < 1ms | Asynchronous |

**Total**: 2-5ms overhead (100% sampling)

**Production**: < 1ms overhead (10% sampling)

## Best Practices

### 1. Sampling Strategy

**Development**: 100% sampling
```yaml
management.tracing.sampling.probability: 1.0
```

**Production**: 10-20% sampling
```yaml
management.tracing.sampling.probability: 0.1
```

**High-traffic**: 1-5% sampling
```yaml
management.tracing.sampling.probability: 0.01
```

### 2. Tag Naming

Use consistent tag names:
- `order.id` - Resource identifiers
- `order.status` - State information
- `result.count` - Result metrics
- `error.type` - Error classification

### 3. Log Levels

**INFO**: Business events (order created, status changed)
**DEBUG**: Detailed flow (fetching, processing)
**WARN**: Recoverable issues (not found, invalid)
**ERROR**: System errors (DB down, timeout)

### 4. Sensitive Data

**Never log**:
- Passwords
- Credit card numbers
- API keys
- Personal identification numbers

**Safe to log**:
- Request IDs
- Trace IDs
- Customer IDs (if allowed)
- Order IDs
- Timestamps
- Durations

### 5. Contextual Information

Always include context in logs:
```java
log.info("Order created successfully: {} for customer: {}", 
         orderId, customerId);
```

Not:
```java
log.info("Order created"); // Missing context
```

## Monitoring & Alerts

### Key Metrics to Monitor

1. **Trace Volume**: Traces per minute
2. **Error Rate**: Traces with errors
3. **Slow Traces**: > 1 second
4. **P95 Latency**: 95th percentile
5. **Span Durations**: Individual operation times

### Alert Rules

```yaml
# Alert on high error rate
- alert: HighErrorRate
  expr: trace_error_rate > 0.05
  for: 5m
  annotations:
    summary: "Error rate > 5% for 5 minutes"

# Alert on slow requests
- alert: SlowRequests
  expr: trace_duration_p95 > 1000
  for: 5m
  annotations:
    summary: "P95 latency > 1 second"
```

## Troubleshooting

### Issue: No Traces in Zipkin

**Causes**:
1. Zipkin not running
2. Wrong endpoint URL
3. Network issues
4. Sampling set to 0

**Solutions**:
```bash
# Check Zipkin is running
curl http://localhost:9411/health

# Check configuration
curl http://localhost:8080/actuator/env | grep zipkin

# Check logs
tail -f logs/application.log | grep zipkin
```

### Issue: Missing Trace Context

**Causes**:
1. Async operations without context propagation
2. Thread pool not configured
3. Manual thread creation

**Solutions**:
```java
// Propagate context to async operations
@Async
public CompletableFuture<OrderResponse> createAsync(OrderCreateRequest request) {
    // Context automatically propagated
    return CompletableFuture.completedFuture(orderService.create(request));
}
```

### Issue: High Memory Usage

**Causes**:
1. 100% sampling in production
2. Large payloads being traced
3. No trace TTL configured

**Solutions**:
```yaml
# Reduce sampling
management.tracing.sampling.probability: 0.1

# Configure Zipkin storage TTL
zipkin:
  storage:
    type: mem
    mem:
      max-spans: 50000
```

## Integration with ELK Stack

### Logstash Configuration

```ruby
input {
  file {
    path => "/var/log/order-service/*.log"
    codec => json
  }
}

filter {
  grok {
    match => { "message" => "%{LOGLEVEL:level} \[%{DATA:application},%{DATA:traceId},%{DATA:spanId}\] \[%{UUID:requestId}\] %{GREEDYDATA:message}" }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "order-service-%{+YYYY.MM.dd}"
  }
}
```

### Kibana Dashboard

Create visualizations:
1. **Request volume**: Requests per minute
2. **Error rate**: Errors over time
3. **Latency**: Response time percentiles
4. **Top errors**: Most common error messages

## Production Checklist

- [x] Tracing implemented
- [x] Custom filters added
- [x] Logging configured
- [x] Sensitive data filtered
- [x] MDC cleanup implemented
- [ ] Zipkin deployed
- [ ] Sampling tuned (10%)
- [ ] Dashboards created
- [ ] Alerts configured
- [ ] Documentation complete
- [ ] Team trained

## Summary

| Feature | Status | Benefit |
|---------|--------|---------|
| Micrometer Tracing | ✅ | Distributed tracing across services |
| Zipkin Integration | ✅ | Visualize request flows |
| Custom Filters | ✅ | Request tracking and logging |
| Structured Logging | ✅ | Searchable, contextual logs |
| Custom Spans | ✅ | Business-specific tracing |
| MDC Context | ✅ | Consistent trace IDs in logs |

**Complete observability for production debugging and monitoring!** 🎯

---

## Additional Resources

- [Micrometer Tracing Docs](https://micrometer.io/docs/tracing)
- [Zipkin Documentation](https://zipkin.io/pages/architecture.html)
- [Spring Boot Observability](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.observability)
- [Distributed Tracing Best Practices](https://opentelemetry.io/docs/concepts/observability-primer/)

