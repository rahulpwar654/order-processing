# Jackson JSR310 (Java 8 Date/Time) Configuration Fix

## Problem

The circuit breaker was triggering with this error:
```
Could not write JSON: Java 8 date/time type `java.time.Instant` not supported by default: 
add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling
```

This occurred when trying to serialize `OrderResponse` objects that contain `java.time.Instant` fields (`createdAt`, `updatedAt`, `canceledAt`).

## Root Cause

The Redis cache configuration was creating a `GenericJackson2JsonRedisSerializer` without the JSR310 module registered, causing Jackson to fail when serializing Java 8 date/time types.

## Solution Applied

### 1. Added Jackson Configuration Bean

Created `JacksonConfig.java` to provide a properly configured `ObjectMapper`:

```java
@Configuration
public class JacksonConfig {
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

**Key features:**
- Registers `JavaTimeModule` to handle Java 8 date/time types
- Disables timestamp serialization (serializes as ISO-8601 strings instead)
- Marked as `@Primary` to be used throughout the application

### 2. Updated CacheConfig

Modified `CacheConfig.java` to inject and use the configured `ObjectMapper`:

```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
    // ...rest of configuration
}
```

### 3. Spring Boot Configuration

Added Jackson configuration in `application.yml`:

```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false
```

## Dependencies

The required dependency is already in `pom.xml`:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

Spring Boot manages the version automatically through its dependency management.

## What Changed

### Before
- Redis cache serializer used default ObjectMapper without JSR310 module
- Attempting to cache `OrderResponse` with `Instant` fields would fail
- Circuit breaker would trip due to repeated serialization failures
- Users received 503 "Service temporarily unavailable"

### After
- All ObjectMapper instances have JSR310 module registered
- Java 8 date/time types serialize correctly as ISO-8601 strings
- Caching works properly for all DTOs
- No more circuit breaker trips due to serialization issues

## Date/Time Serialization Format

With this configuration, `Instant` fields are serialized as ISO-8601 strings:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2025-10-30T18:43:45.123Z",
  "updatedAt": "2025-10-30T18:43:45.123Z",
  "canceledAt": null
}
```

Instead of Unix timestamps:
```json
{
  "createdAt": 1730315025123
}
```

## Testing

To verify the fix:

1. Start the application
2. Create an order:
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

3. Verify successful response with properly formatted dates:
```json
{
  "id": "...",
  "customerId": "CUST123",
  "status": "PENDING",
  "createdAt": "2025-10-30T18:43:45.123Z",
  "updatedAt": "2025-10-30T18:43:45.123Z",
  ...
}
```

4. Check circuit breaker status:
```bash
curl http://localhost:8080/actuator/circuitbreakers
```

Should show `CLOSED` state.

## Additional Benefits

This configuration also benefits:

1. **REST API responses** - Consistent date formatting across all endpoints
2. **Redis caching** - All DTOs with date/time fields can be cached
3. **Logging** - Date/time values in logs are human-readable
4. **API clients** - Standard ISO-8601 format is universally supported

## Common Issues

### Issue: Dates still serializing as timestamps
**Solution:** Ensure `write-dates-as-timestamps: false` is set in `application.yml`

### Issue: Custom ObjectMapper not being used
**Solution:** Verify `@Primary` annotation is on the `ObjectMapper` bean

### Issue: Redis still failing
**Solution:** 
- Check Redis is running: `redis-cli ping`
- Review cache error logs (CacheErrorHandler logs warnings)
- Temporarily disable cache: `spring.cache.type=none`

## Related Files

- `src/main/java/com/example/order/config/JacksonConfig.java` - ObjectMapper configuration
- `src/main/java/com/example/order/config/CacheConfig.java` - Redis cache configuration
- `src/main/resources/application.yml` - Spring Jackson properties
- `src/main/java/com/example/order/dto/OrderResponse.java` - DTO with Instant fields

## References

- [Jackson JSR310 Module Documentation](https://github.com/FasterXML/jackson-modules-java8)
- [Spring Boot Jackson Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.spring-mvc.customize-jackson-objectmapper)
- [ISO-8601 Date Format](https://en.wikipedia.org/wiki/ISO_8601)

