# Service Discovery & Load Balancing - Complete Setup

## ✅ IMPLEMENTATION COMPLETE

Successfully integrated **Netflix Eureka** for service discovery and **Spring Cloud LoadBalancer** for client-side load balancing.

## 📋 What Was Implemented

### 1. Eureka Server (Service Registry)
- **Location:** `eureka-server/`
- **Port:** 8761
- **Dashboard:** http://localhost:8761
- **Purpose:** Central registry for all microservices

### 2. Eureka Client (Order Service)
- **Service Name:** `order`
- **Features:**
  - Auto-registration with Eureka
  - Service discovery
  - Health monitoring
  - Heartbeat mechanism

### 3. Load Balancer
- **Type:** Client-side load balancing
- **Strategy:** Round-robin (default)
- **Clients:**
  - RestTemplate (synchronous)
  - WebClient (reactive/async)

### 4. Service Discovery API
- **New Endpoints:**
  - `GET /api/discovery/services` - List all services
  - `GET /api/discovery/services/{name}` - List service instances
  - `GET /api/discovery/info` - Current service info

### 5. Health Indicators
- Custom health check for service discovery
- Integrated with Spring Boot Actuator
- Visible at `/actuator/health`

## 🚀 Quick Start

### Option 1: Using Startup Scripts (Windows)

**Step 1: Start Eureka Server**
```bash
start-eureka-server.bat
```
Wait ~30 seconds, then verify: http://localhost:8761

**Step 2: Start Order Service Instance 1**
```bash
start-order-service-1.bat
```

**Step 3: Start More Instances (Optional)**
```bash
start-order-service-2.bat
start-order-service-3.bat
```

### Option 2: Manual Start

**Terminal 1: Eureka Server**
```bash
cd eureka-server
mvn spring-boot:run
```

**Terminal 2: Order Service**
```bash
cd D:\java\order-processing-java
mvn spring-boot:run
```

**Terminal 3: Order Service Instance 2**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

## 🧪 Testing

### 1. Verify Eureka Dashboard
Open: http://localhost:8761

**Expected:**
- Application: **ORDER**
- Status: **UP** (green)
- Instances: 1 or more

### 2. Test Service Discovery API

```bash
# List all services
curl http://localhost:8080/api/discovery/services

# View order service instances
curl http://localhost:8080/api/discovery/services/order

# Get current service info
curl http://localhost:8080/api/discovery/info
```

### 3. Check Health Status

```bash
curl http://localhost:8080/actuator/health
```

**Look for:**
```json
{
  "serviceDiscovery": {
    "status": "UP",
    "details": {
      "services-discovered": 1,
      "service-names": ["order"]
    }
  }
}
```

### 4. Test Load Balancing

**Start multiple instances, then:**

```bash
# Windows PowerShell
for ($i=1; $i -le 10; $i++) {
    Write-Host "Request $i"
    curl http://localhost:8080/api/discovery/info
}

# Linux/Mac
for i in {1..10}; do
    echo "Request $i"
    curl http://localhost:8080/api/discovery/info
    sleep 0.5
done
```

**Expected:** Round-robin distribution across instances

## 📁 Files Created

### Eureka Server
1. `eureka-server/pom.xml`
2. `eureka-server/src/main/java/com/example/eureka/EurekaServerApplication.java`
3. `eureka-server/src/main/resources/application.yml`

### Order Service
4. `src/main/java/com/example/order/config/LoadBalancerConfig.java`
5. `src/main/java/com/example/order/health/ServiceDiscoveryHealthIndicator.java`
6. `src/main/java/com/example/order/controller/ServiceDiscoveryController.java`

### Startup Scripts
7. `start-eureka-server.bat`
8. `start-order-service-1.bat`
9. `start-order-service-2.bat`
10. `start-order-service-3.bat`

### Documentation
11. `doc/SERVICE_DISCOVERY_LOADBALANCING.md` - Full documentation
12. `doc/SERVICE_DISCOVERY_QUICKSTART.md` - Quick start guide
13. `doc/SERVICE_DISCOVERY_SETUP.md` - This file

## 📝 Configuration Changes

### pom.xml (Order Service)
```xml
<!-- Added Dependencies -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### application.yml (Order Service)
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
    instance-id: ${spring.application.name}:${random.value}
```

### OrderApplication.java
```java
@EnableDiscoveryClient  // Added annotation
```

## 🏗️ Architecture

```
┌─────────────────────┐
│  Eureka Server      │
│  (Port 8761)        │
│  Service Registry   │
└──────────┬──────────┘
           │
           │ Register & Discover
           │
    ┌──────┴───────┬─────────┬─────────┐
    │              │         │         │
┌───▼────┐    ┌───▼────┐ ┌──▼─────┐   │
│ Order  │    │ Order  │ │ Order  │   │
│ :8080  │    │ :8081  │ │ :8082  │   │
└────────┘    └────────┘ └────────┘   │
                                      │
                              ┌───────▼────────┐
                              │ Load Balancer  │
                              │ (Client-Side)  │
                              └────────────────┘
```

## 🔧 How It Works

### Service Registration
1. Order service starts
2. Registers with Eureka at `http://localhost:8761/eureka/`
3. Sends heartbeat every 5 seconds
4. Eureka marks as UP (green)

### Service Discovery
1. Service queries Eureka for other services
2. Gets list of available instances
3. Caches locally for performance
4. Refreshes every 5 seconds

### Load Balancing
1. Client makes request to `http://order/api/endpoint`
2. LoadBalancer resolves "order" to list of instances
3. Selects instance using round-robin
4. Forwards request to selected instance
5. Handles failover if instance is down

## 🎯 Use Cases

### 1. Horizontal Scaling
```bash
# Add more instances easily
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8084"
```

### 2. Zero-Downtime Deployments
```bash
# Deploy new version to one instance
# Old instances continue serving requests
# Gradually replace all instances
```

### 3. Automatic Failover
```bash
# If instance 8081 crashes:
# - Eureka detects missed heartbeats (10 sec)
# - Marks instance as DOWN
# - LoadBalancer routes to healthy instances
# - No manual intervention needed
```

### 4. Inter-Service Communication
```java
@Autowired
private RestTemplate restTemplate;

// Call other services by name, not host:port
OrderResponse order = restTemplate.getForObject(
    "http://order/api/orders/123",
    OrderResponse.class
);
```

## 📊 Monitoring

### Eureka Dashboard
**URL:** http://localhost:8761

**Metrics:**
- Registered services
- Instance status (UP/DOWN)
- Last heartbeat
- Number of renewals
- Available/Unavailable replicas

### Actuator Endpoints
```bash
# Health check with service discovery
curl http://localhost:8080/actuator/health

# All registered services
curl http://localhost:8761/eureka/apps

# Specific service
curl http://localhost:8761/eureka/apps/ORDER

# Metrics
curl http://localhost:8080/actuator/metrics
```

### Swagger UI
**URL:** http://localhost:8080/swagger-ui.html

**New Tag:** Service Discovery
- GET /api/discovery/services
- GET /api/discovery/services/{serviceName}
- GET /api/discovery/info

## 🔍 Troubleshooting

### Issue: Service Not Registering

**Symptoms:** Service starts but doesn't appear in Eureka

**Solutions:**
1. Verify Eureka server is running: http://localhost:8761
2. Check `register-with-eureka: true` in application.yml
3. Check logs for Eureka connection errors
4. Verify network connectivity

```bash
# Check logs
grep -i "eureka" logs/application.log

# Test Eureka server
curl http://localhost:8761/actuator/health
```

### Issue: Load Balancing Not Working

**Symptoms:** All requests go to same instance

**Solutions:**
1. Verify multiple instances registered in Eureka
2. Check `@LoadBalanced` annotation on RestTemplate
3. Use service name (e.g., `http://order`) not host:port

```bash
# Verify instances
curl http://localhost:8080/api/discovery/services/order
```

### Issue: Instance Marked as DOWN

**Symptoms:** Red status in Eureka Dashboard

**Solutions:**
1. Check application is actually running
2. Verify actuator health endpoint: `/actuator/health`
3. Check firewall rules
4. Review heartbeat configuration

```bash
# Test health endpoint
curl http://localhost:8080/actuator/health
```

## 🎓 Key Concepts

### Service Registry Pattern
- Central registry (Eureka) tracks all services
- Services register on startup
- Services discover each other via registry
- Eliminates hardcoded IP addresses

### Client-Side Load Balancing
- Load balancer runs in client JVM
- No central load balancer (no single point of failure)
- Faster (no network hop)
- More resilient

### Heartbeat Mechanism
- Services send heartbeat every 5 seconds
- Eureka expects heartbeat within 10 seconds
- Missed heartbeats → mark as DOWN
- Automatic deregistration

## 🔒 Production Considerations

### 1. Multiple Eureka Servers
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka1:8761/eureka/,http://eureka2:8762/eureka/
```

### 2. Enable Self-Preservation
```yaml
eureka:
  server:
    enable-self-preservation: true
```

### 3. HTTPS
```yaml
eureka:
  instance:
    secure-port-enabled: true
    non-secure-port-enabled: false
```

### 4. Authentication
```yaml
spring:
  security:
    user:
      name: admin
      password: ${EUREKA_PASSWORD}
```

## 📚 Documentation

- **Full Guide:** [SERVICE_DISCOVERY_LOADBALANCING.md](SERVICE_DISCOVERY_LOADBALANCING.md)
- **Quick Start:** [SERVICE_DISCOVERY_QUICKSTART.md](SERVICE_DISCOVERY_QUICKSTART.md)
- **Spring Cloud Netflix:** https://spring.io/projects/spring-cloud-netflix
- **LoadBalancer Guide:** https://spring.io/guides/gs/spring-cloud-loadbalancer

## ✅ Success Checklist

- [x] Eureka Server created and configured
- [x] Eureka Client dependency added
- [x] LoadBalancer dependency added
- [x] Service registration configured
- [x] Service discovery controller implemented
- [x] Health indicators added
- [x] LoadBalanced RestTemplate configured
- [x] LoadBalanced WebClient configured
- [x] Startup scripts created
- [x] Documentation completed
- [x] Swagger UI updated
- [x] No compilation errors

## 🎉 Benefits

1. **Dynamic Scaling** - Add/remove instances easily
2. **High Availability** - Automatic failover
3. **Zero-Downtime** - Rolling deployments
4. **Service Isolation** - Services find each other automatically
5. **Better Performance** - Client-side load balancing
6. **Fault Tolerance** - Circuit breaker integration
7. **Monitoring** - Built-in dashboard and metrics

## 🚀 Next Steps

1. **Start the system:**
   ```bash
   start-eureka-server.bat
   start-order-service-1.bat
   ```

2. **Test service discovery:**
   ```bash
   curl http://localhost:8080/api/discovery/info
   ```

3. **Add more services:**
   - Payment Service
   - Inventory Service
   - Notification Service
   - All discover each other via Eureka

4. **Add API Gateway:**
   - Spring Cloud Gateway
   - Routes requests to services
   - Single entry point for clients

5. **Configure monitoring:**
   - Prometheus metrics
   - Grafana dashboards
   - Alert rules

---

**Service Discovery & Load Balancing: FULLY OPERATIONAL!** 🎊

