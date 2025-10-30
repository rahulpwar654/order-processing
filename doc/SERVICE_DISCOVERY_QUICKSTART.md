# Quick Start - Service Discovery & Load Balancing

## What Was Added

✅ **Netflix Eureka Server** - Service Registry  
✅ **Eureka Client** - Service Registration  
✅ **Spring Cloud LoadBalancer** - Client-Side Load Balancing  
✅ **Service Discovery Controller** - View registered services  
✅ **Health Indicators** - Monitor service discovery

## Start the System

### Step 1: Start Eureka Server

```bash
cd eureka-server
mvn spring-boot:run
```

**Verify:** Open http://localhost:8761  
You should see Eureka Dashboard (may be empty initially)

### Step 2: Start Order Service (Instance 1)

```bash
# New terminal
cd D:\java\order-processing-java
mvn spring-boot:run
```

**Verify:** 
- Check Eureka Dashboard at http://localhost:8761
- You should see "ORDER" service with 1 instance
- Status should be UP (green)

### Step 3: Start Additional Instances (Optional)

**Instance 2:**
```bash
# New terminal
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

**Instance 3:**
```bash
# New terminal
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

**Verify:** Eureka Dashboard shows 3 instances of ORDER service

## Test Service Discovery

### View Registered Services

```bash
curl http://localhost:8080/api/discovery/services
```

**Expected:**
```json
["order"]
```

### View Service Instances

```bash
curl http://localhost:8080/api/discovery/services/order
```

**Expected:**
```json
[
  {
    "instanceId": "order:12345",
    "host": "192.168.1.100",
    "port": 8080,
    "uri": "http://192.168.1.100:8080",
    "secure": false
  }
]
```

### View Current Service Info

```bash
curl http://localhost:8080/api/discovery/info
```

## Test Load Balancing

### Test Script

```bash
# Windows (PowerShell)
for ($i=1; $i -le 10; $i++) {
    Write-Host "Request $i"
    curl http://localhost:8080/api/discovery/info
    Start-Sleep -Seconds 1
}

# Linux/Mac (Bash)
for i in {1..10}; do
    echo "Request $i"
    curl http://localhost:8080/api/discovery/info
    sleep 1
done
```

**Expected:** Requests distributed across all instances (round-robin)

## Check Health Status

```bash
curl http://localhost:8080/actuator/health
```

**Expected:**
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
    }
  }
}
```

## Eureka Dashboard

Open: http://localhost:8761

**What You'll See:**
- **Instances currently registered with Eureka**
  - Application: ORDER
  - Status: UP (1) - or more if multiple instances
  - Availability Zones: zone1

- **General Info**
  - Environment: test
  - Data center: default

- **Instance Info** (click on instance link)
  - Instance ID
  - Status: UP
  - IP Address
  - Metadata

## Swagger UI Update

Open: http://localhost:8080/swagger-ui.html

**New Endpoints Added:**
- **Service Discovery** tag
  - GET /api/discovery/services
  - GET /api/discovery/services/{serviceName}
  - GET /api/discovery/info

## Test Failover

### Step 1: Start 2 Instances
```bash
# Terminal 1
mvn spring-boot:run

# Terminal 2  
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Step 2: Send Continuous Requests
```bash
# Terminal 3
while true; do curl -s http://localhost:8080/api/discovery/info | jq '.instanceCount'; sleep 1; done
```

**Expected:** Alternating between 2 instances

### Step 3: Kill One Instance
Press Ctrl+C in Terminal 2 (port 8081)

**Expected:** 
- After 10-15 seconds, instance count drops to 1
- All requests now go to remaining instance
- No errors in client

### Step 4: Restart Instance
Start Terminal 2 again

**Expected:**
- After 5-10 seconds, instance count back to 2
- Requests resume load balancing

## Port Configuration

| Component | Port | URL |
|-----------|------|-----|
| Eureka Server | 8761 | http://localhost:8761 |
| Order Service (Instance 1) | 8080 | http://localhost:8080 |
| Order Service (Instance 2) | 8081 | http://localhost:8081 |
| Order Service (Instance 3) | 8082 | http://localhost:8082 |

## Configuration Files

### Eureka Server
- **Location:** `eureka-server/src/main/resources/application.yml`
- **Port:** 8761
- **Key Settings:**
  - `register-with-eureka: false` (server doesn't register itself)
  - `fetch-registry: false` (server doesn't fetch registry)

### Order Service
- **Location:** `src/main/resources/application.yml`
- **Service Name:** `order`
- **Key Settings:**
  - `register-with-eureka: true` (client registers)
  - `fetch-registry: true` (client discovers others)
  - `defaultZone: http://localhost:8761/eureka/`

## Success Criteria

- ✅ Eureka Server running on port 8761
- ✅ Eureka Dashboard accessible
- ✅ Order service registers successfully
- ✅ Service appears in Eureka Dashboard (green/UP)
- ✅ Service discovery endpoints work
- ✅ Health check shows service discovery as UP
- ✅ Multiple instances distribute load
- ✅ Failed instances automatically removed
- ✅ Documentation updated in Swagger

## Troubleshooting

### Eureka Server Not Starting
```bash
# Check if port 8761 is available
netstat -ano | findstr :8761

# Check logs
cd eureka-server
mvn spring-boot:run
# Look for errors in console
```

### Service Not Registering
```bash
# Check application logs
grep -i "eureka" logs/application.log

# Verify Eureka server is running
curl http://localhost:8761/actuator/health

# Check configuration
cat src/main/resources/application.yml | grep -A 10 "eureka:"
```

### Load Balancing Not Working
1. Ensure multiple instances are registered in Eureka
2. Verify using service name (e.g., `http://order`) not `localhost:8080`
3. Check `@LoadBalanced` annotation is on RestTemplate/WebClient

## Next Steps

1. **Build the project:**
   ```bash
   mvn clean package
   ```

2. **Start in production mode:**
   ```bash
   java -jar target/order-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```

3. **Add API Gateway:**
   - Spring Cloud Gateway
   - Route requests through gateway
   - Gateway discovers services via Eureka

4. **Configure multiple Eureka servers:**
   - High availability
   - Peer-to-peer replication

## Documentation

- **Full Guide:** `doc/SERVICE_DISCOVERY_LOADBALANCING.md`
- **Architecture Diagrams:** See documentation
- **Code Examples:** See documentation

## Key Features

| Feature | Status |
|---------|--------|
| Service Registration | ✅ Implemented |
| Service Discovery | ✅ Implemented |
| Client-Side Load Balancing | ✅ Implemented |
| Health Monitoring | ✅ Implemented |
| Automatic Failover | ✅ Implemented |
| Dashboard UI | ✅ Implemented |
| REST API for Discovery | ✅ Implemented |
| Swagger Documentation | ✅ Implemented |

