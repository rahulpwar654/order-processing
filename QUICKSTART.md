# 🚀 Quick Start - Order Processing Service

## ⚡ START THE APPLICATION RIGHT NOW

### Option 1: Double-click the startup script
```
start-order-service-1.bat
```

### Option 2: Run this command
```cmd
mvn spring-boot:run -Dspring.profiles.active=standalone
```

**That's it!** The application will start on port 8080.

---

## 🌐 Access the Application

After starting (wait 30 seconds):

- **API Base:** http://localhost:8080/api/orders
- **Swagger UI:** http://localhost:8080/swagger-ui.html  
- **Health Check:** http://localhost:8080/actuator/health
- **Eureka Dashboard:** http://localhost:8761 (if running with Eureka)

---

## 📚 Documentation

- **[WORKING_COMMANDS.md](WORKING_COMMANDS.md)** - ⭐ **READ THIS FIRST** - Correct commands
- **[CORRECT_COMMANDS.md](CORRECT_COMMANDS.md)** - Complete command reference
- **[EUREKA_FIX_README.md](EUREKA_FIX_README.md)** - Eureka connection fix
- **[doc/](doc/)** - Complete documentation library

---

## 🎯 Key Features Implemented

✅ Order CRUD Operations  
✅ HATEOAS (Richardson Level 3)  
✅ Idempotency for Order Creation  
✅ Circuit Breaker (Resilience4j)  
✅ Rate Limiting  
✅ Redis Caching  
✅ Distributed Tracing (Zipkin)  
✅ Service Discovery (Eureka)  
✅ Client-Side Load Balancing  
✅ Swagger/OpenAPI Documentation  
✅ Health Checks & Monitoring  

---

## 🔥 Common Tasks

### Start Standalone (No Eureka)
```cmd
start-order-service-1.bat
```

### Start with Service Discovery
```cmd
start-all.bat
```

### Create an Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-key-123" \
  -d '{
    "customerId": "CUST123",
    "items": [{
      "productId": "PROD456",
      "quantity": 2,
      "unitPrice": 149.99
    }]
  }'
```

### Get All Orders
```bash
curl http://localhost:8080/api/orders
```

### View Service Discovery
```bash
curl http://localhost:8080/api/discovery/services
```

---

## ❓ Need Help?

**Problem starting the application?**  
→ Read [WORKING_COMMANDS.md](WORKING_COMMANDS.md)

**Want to understand service discovery?**  
→ Read [doc/SERVICE_DISCOVERY_LOADBALANCING.md](doc/SERVICE_DISCOVERY_LOADBALANCING.md)

**Eureka connection issues?**  
→ Read [doc/EUREKA_TROUBLESHOOTING.md](doc/EUREKA_TROUBLESHOOTING.md)

**Want to see all features?**  
→ Check [doc/](doc/) folder for complete documentation

---

## 🏗️ Project Structure

```
order-processing-java/
├── src/main/java/           # Application source code
├── eureka-server/            # Service discovery server
├── doc/                      # Complete documentation
├── start-*.bat              # Startup scripts
├── WORKING_COMMANDS.md      # ⭐ Command reference
└── pom.xml                  # Maven dependencies
```

---

## ✅ Startup Scripts

| Script | Description |
|--------|-------------|
| `start-order-service-1.bat` | Start standalone (port 8080) |
| `start-order-service-2.bat` | Start instance 2 (port 8081) |
| `start-order-service-3.bat` | Start instance 3 (port 8082) |
| `start-order-service-with-eureka.bat` | Start with Eureka |
| `start-eureka-server.bat` | Start Eureka Server |
| `start-all.bat` | Start everything |

---

## 🎓 Technology Stack

- **Java 21**
- **Spring Boot 3.5.7**
- **Spring Cloud 2025.0.0**
- **Netflix Eureka** - Service Discovery
- **Resilience4j** - Circuit Breaker & Rate Limiting
- **Redis** - Caching
- **H2** - In-memory Database
- **Zipkin** - Distributed Tracing
- **Swagger/OpenAPI** - API Documentation
- **Spring HATEOAS** - Hypermedia APIs

---

**Ready to start? Run: `start-order-service-1.bat`** 🚀

