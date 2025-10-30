# Service Discovery and Load Balancing Implementation

## Overview

This implementation adds **Netflix Eureka** for service discovery and **Spring Cloud LoadBalancer** for client-side load balancing to the Order Processing System.

## Architecture

```
┌─────────────────────┐
│  Eureka Server      │
│  (Port 8761)        │
│  Service Registry   │
└──────────┬──────────┘
           │
           │ Register/Discover
           │
    ┌──────┴───────┬─────────┬─────────┐
    │              │         │         │
┌───▼────┐    ┌───▼────┐ ┌──▼─────┐  │
│ Order  │    │ Order  │ │ Order  │  │
│ Inst 1 │    │ Inst 2 │ │ Inst 3 │  │
│ :8080  │    │ :8081  │ │ :8082  │  │
└────────┘    └────────┘ └────────┘  │
                                     │
                              ┌──────▼──────┐
                              │ API Gateway │
                              │  (Future)   │
                              └─────────────┘
```

## Components

### 1. Eureka Server (Service Registry)
**Location:** `eureka-server/`

- **Port:** 8761
- **Dashboard:** http://localhost:8761
- **Purpose:** Central registry for all microservices

**Features:**
- Service registration
- Service health monitoring
- Service instance tracking
- Automatic deregistration of failed instances

### 2. Order Service (Eureka Client)
**Location:** `src/main/java/com/example/order/`

- **Service Name:** `order`
- **Port:** 8080 (configurable)
- **Purpose:** Registers with Eureka and can discover other services

**Features:**
- Auto-registration on startup
- Heartbeat monitoring
- Load-balanced service calls
- Health check integration

## Configuration

### Eureka Server Configuration

**File:** `eureka-server/src/main/resources/application.yml`

```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false  # Don't register itself
    fetch-registry: false         # Don't fetch registry
  server:
    enable-self-preservation: false  # Disabled in dev
```

### Order Service Configuration

**File:** `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: order  # Service name in registry

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 5
    instance-id: ${spring.application.name}:${random.value}
```

## How to Run

### Step 1: Start Eureka Server

```bash
cd eureka-server
mvn spring-boot:run
```

**Verify:** Open http://localhost:8761

You should see the Eureka Dashboard.

### Step 2: Start Order Service Instance 1

```bash
# Terminal 1
cd D:\java\order-processing-java
mvn spring-boot:run
```

**Verify:** Check Eureka Dashboard at http://localhost:8761
- You should see "ORDER" service registered

### Step 3: Start Order Service Instance 2 (Optional)

```bash
# Terminal 2
cd D:\java\order-processing-java
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Step 4: Start Order Service Instance 3 (Optional)

```bash
# Terminal 3
cd D:\java\order-processing-java
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

**Verify:** Eureka Dashboard should show 3 instances of "ORDER" service

## Service Discovery Endpoints

### View All Registered Services

```bash
curl http://localhost:8080/api/discovery/services
```

**Response:**
```json
["order"]
```

### View All Instances of Order Service

```bash
curl http://localhost:8080/api/discovery/services/order
```

**Response:**
```json
[
  {
    "instanceId": "order:12345",
    "host": "192.168.1.100",
    "port": 8080,
    "uri": "http://192.168.1.100:8080",
    "metadata": {
      "zone": "zone1",
      "version": "1.0.0"
    },
    "secure": false
  },
  {
    "instanceId": "order:67890",
    "host": "192.168.1.100",
    "port": 8081,
    "uri": "http://192.168.1.100:8081",
    "metadata": {
      "zone": "zone1",
      "version": "1.0.0"
    },
    "secure": false
  }
]
```

### View Current Service Info

```bash
curl http://localhost:8080/api/discovery/info
```

**Response:**
```json
{
  "serviceName": "order",
  "instanceCount": 3,
  "instances": [...],
  "allServices": ["order"]
}
```

## Load Balancing

### Using RestTemplate (Synchronous)

```java
@Autowired
private RestTemplate restTemplate;

public OrderResponse callAnotherService() {
    // Service name instead of host:port
    return restTemplate.getForObject(
        "http://order/api/orders/123",
        OrderResponse.class
    );
}
```

**How it works:**
1. `http://order` - Uses service name, not host/port
2. LoadBalancer resolves to actual instance
3. Round-robin distribution across instances
4. Automatic failover if instance is down

### Using WebClient (Reactive)

```java
@Autowired
private WebClient.Builder webClientBuilder;

public Mono<OrderResponse> callAnotherServiceAsync() {
    return webClientBuilder.build()
        .get()
        .uri("http://order/api/orders/123")
        .retrieve()
        .bodyToMono(OrderResponse.class);
}
```

## Load Balancing Strategies

Default strategy is **Round Robin**, but you can configure:

### 1. Round Robin (Default)
Distributes requests evenly across all instances.

### 2. Random
Randomly selects an instance.

### 3. Weighted Response Time
Considers instance response time.

### Configuration Example

```java
@Configuration
@LoadBalancerClient(name = "order", configuration = CustomLoadBalancerConfiguration.class)
public class LoadBalancerConfig {
    // ...
}

class CustomLoadBalancerConfiguration {
    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            LoadBalancerClientFactory loadBalancerClientFactory) {
        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
            loadBalancerClientFactory.getLazyProvider(name, ServiceInstanceListSupplier.class),
            name);
    }
}
```

## Health Checks

### Service Discovery Health

```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "serviceDiscovery": {
      "status": "UP",
      "details": {
        "services-discovered": 1,
        "service-names": ["order"],
        "status": "Connected to Eureka Server"
      }
    },
    "diskSpace": {...},
    "ping": {...}
  }
}
```

### Eureka Server Health

```bash
curl http://localhost:8761/actuator/health
```

## Monitoring

### Eureka Dashboard
**URL:** http://localhost:8761

**Shows:**
- All registered services
- Instance status (UP, DOWN, OUT_OF_SERVICE)
- Last heartbeat time
- Number of renewals
- Available/Unavailable replicas

### Service Instances

Navigate to Eureka Dashboard to see:
- **Application Name:** ORDER
- **AMIs:** Number of instances
- **Availability Zones:** zone1
- **Status:** UP (1), DOWN (0)

### Actuator Endpoints

**Order Service:**
```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Environment
curl http://localhost:8080/actuator/env
```

**Eureka Server:**
```bash
# Registered applications
curl http://localhost:8761/eureka/apps

# Specific service
curl http://localhost:8761/eureka/apps/ORDER
```

## Benefits

### 1. Dynamic Service Discovery
- No hardcoded IP addresses/ports
- Services find each other automatically
- Easy to add/remove instances

### 2. Load Balancing
- Client-side load balancing
- No single point of failure
- Better performance distribution

### 3. Fault Tolerance
- Automatic health checking
- Failed instances automatically removed
- Requests routed to healthy instances

### 4. Scalability
- Add instances dynamically
- Horizontal scaling made easy
- Zero-downtime deployments

### 5. Service Isolation
- Services communicate via logical names
- Physical location abstracted
- Easy to move services between servers

## Testing Load Balancing

### Test Script (Bash)

```bash
#!/bin/bash

# Run 20 requests and see which instance handles them
for i in {1..20}; do
  echo "Request $i:"
  curl -s http://localhost:8080/api/orders | jq -r '.port'
  sleep 0.5
done
```

### Expected Output

```
Request 1: 8080
Request 2: 8081
Request 3: 8082
Request 4: 8080  # Round-robin back to first
Request 5: 8081
...
```

### Test Failover

```bash
# 1. Start 3 instances (8080, 8081, 8082)
# 2. Send requests continuously
while true; do curl http://localhost:8080/api/discovery/info; sleep 1; done

# 3. Kill one instance (Ctrl+C on port 8081)
# 4. Observe: Requests now only go to 8080 and 8082
# 5. Start 8081 again
# 6. Observe: Requests resume to all 3 instances
```

## Multi-Environment Configuration

### Development (application-dev.yml)

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  server:
    enable-self-preservation: false
```

### Production (application-prod.yml)

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka1.prod.com:8761/eureka/,http://eureka2.prod.com:8761/eureka/
  server:
    enable-self-preservation: true
  instance:
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30
```

## Troubleshooting

### Issue 1: Service Not Registering

**Symptoms:**
- Service starts but doesn't appear in Eureka Dashboard

**Solutions:**
1. Check Eureka server is running at port 8761
2. Verify `eureka.client.register-with-eureka=true`
3. Check network connectivity to Eureka server
4. Look for errors in logs:
   ```bash
   grep -i "eureka" logs/application.log
   ```

### Issue 2: Load Balancing Not Working

**Symptoms:**
- All requests go to same instance

**Solutions:**
1. Verify `@LoadBalanced` annotation on RestTemplate/WebClient
2. Check multiple instances are registered in Eureka
3. Ensure using service name (e.g., `http://order`) not host:port

### Issue 3: Instance Shows as DOWN

**Symptoms:**
- Instance appears red in Eureka Dashboard

**Solutions:**
1. Check actuator health endpoint is accessible
2. Verify `eureka.instance.health-check-url-path=/actuator/health`
3. Ensure application is actually healthy
4. Check firewall rules

### Issue 4: Slow Registration

**Symptoms:**
- Long delay before service appears in Eureka

**Solutions:**
- Reduce intervals in development:
  ```yaml
  eureka:
    instance:
      lease-renewal-interval-in-seconds: 5
    client:
      registry-fetch-interval-seconds: 5
  ```

## Security Considerations

### Production Recommendations

1. **Enable HTTPS:**
   ```yaml
   eureka:
     instance:
       secure-port-enabled: true
       non-secure-port-enabled: false
   ```

2. **Add Authentication:**
   ```yaml
   spring:
     security:
       user:
         name: admin
         password: ${EUREKA_PASSWORD}
   ```

3. **Network Segmentation:**
   - Place Eureka server in private network
   - Use VPN or private cloud network

4. **Metadata Encryption:**
   - Don't store sensitive data in metadata
   - Use secure configuration management

## Future Enhancements

1. **API Gateway Integration**
   - Spring Cloud Gateway
   - Zuul 2.x

2. **Multiple Eureka Servers**
   - High availability setup
   - Peer-to-peer replication

3. **Service Mesh Integration**
   - Istio
   - Linkerd

4. **Advanced Load Balancing**
   - Custom load balancing rules
   - Zone-aware load balancing

5. **Circuit Breaker Integration**
   - Already implemented with Resilience4j
   - Integrate with service discovery

## Related Files

- **Eureka Server:** `eureka-server/`
- **Load Balancer Config:** `src/main/java/com/example/order/config/LoadBalancerConfig.java`
- **Service Discovery Controller:** `src/main/java/com/example/order/controller/ServiceDiscoveryController.java`
- **Health Indicator:** `src/main/java/com/example/order/health/ServiceDiscoveryHealthIndicator.java`
- **Configuration:** `src/main/resources/application.yml`

## References

- [Spring Cloud Netflix Eureka](https://spring.io/projects/spring-cloud-netflix)
- [Spring Cloud LoadBalancer](https://spring.io/guides/gs/spring-cloud-loadbalancer)
- [Eureka REST Operations](https://github.com/Netflix/eureka/wiki/Eureka-REST-operations)
- [Microservices Patterns](https://microservices.io/patterns/service-registry.html)

