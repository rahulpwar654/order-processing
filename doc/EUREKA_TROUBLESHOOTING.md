# Troubleshooting: Eureka Connection Refused

## Problem

When running `mvn spring-boot:run`, you see:

```
Request execution error. endpoint=DefaultEndpoint{ serviceUrl='http://localhost:8761/eureka/}
exception=I/O error on GET request for "http://localhost:8761/eureka/apps/": 
Connect to http://localhost:8761 failed: Connection refused: getsockopt
```

## Root Cause

The Order Service is trying to connect to Eureka Server at `http://localhost:8761`, but **Eureka Server is not running**.

## Solutions

### Option 1: Run in Standalone Mode (WITHOUT Eureka)

If you want to run the Order Service **without** service discovery:

#### Windows:
```bash
start-order-service-1.bat
```

#### Manual:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=standalone"
```

#### Or set in IDE:
**IntelliJ IDEA / Eclipse:**
- VM Options: `-Dspring.profiles.active=standalone`
- Or Environment Variable: `SPRING_PROFILES_ACTIVE=standalone`

**What this does:**
- Disables Eureka client
- Service runs on port 8080
- No service discovery or load balancing
- All other features work normally

---

### Option 2: Run WITH Eureka (Full Service Discovery)

If you want service discovery and load balancing enabled:

#### Step 1: Start Eureka Server FIRST

**Terminal 1:**
```bash
cd eureka-server
mvn spring-boot:run
```

**Wait** until you see:
```
Started EurekaServerApplication in X.XXX seconds
```

**Verify:** Open http://localhost:8761 (Eureka Dashboard should load)

#### Step 2: Start Order Service

**Terminal 2:**
```bash
start-order-service-with-eureka.bat
```

**Or manually:**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=eureka"
```

**Verify:** 
- Check Eureka Dashboard at http://localhost:8761
- You should see "ORDER" service registered (green/UP)

---

### Option 3: Start Everything at Once

Use the master startup script:

```bash
start-all.bat
```

**What this does:**
1. Starts Eureka Server in new window
2. Waits 30 seconds
3. Starts Order Service in new window
4. Both services communicate automatically

---

## Quick Fix Commands

### If you just want to run the app NOW:

```bash
# Standalone mode (no Eureka needed)
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=standalone"
```

### If you want to test service discovery:

```bash
# Terminal 1: Start Eureka
cd eureka-server
mvn spring-boot:run

# Wait 30 seconds, then in Terminal 2:
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=eureka"
```

---

## Application Profiles Explained

### Profile: `standalone` (Default)
```yaml
# Eureka disabled
eureka.client.enabled: false
spring.cloud.discovery.enabled: false
```

**Use when:**
- Developing locally without Eureka
- Running single instance
- Testing without service discovery

**Start command:**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=standalone"
```

### Profile: `eureka`
```yaml
# Eureka enabled
eureka.client.enabled: true
eureka.client.register-with-eureka: true
```

**Use when:**
- Eureka Server is running
- Testing service discovery
- Running multiple instances
- Testing load balancing

**Prerequisites:**
- Eureka Server must be running at http://localhost:8761

**Start command:**
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=eureka"
```

### No Profile (Default behavior)
```yaml
# Tries to connect to Eureka but won't fail if unavailable
eureka.client.register-with-eureka: true
```

**Behavior:**
- Attempts to connect to Eureka
- Shows warnings if Eureka not available
- Service still runs but without discovery

---

## Verification Steps

### After Starting Standalone Mode:

1. **Check application started:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   Should return: `{"status":"UP"}`

2. **Test API:**
   ```bash
   curl http://localhost:8080/api/discovery/services
   ```
   Should return: `[]` (empty, no Eureka)

3. **Access Swagger:**
   Open: http://localhost:8080/swagger-ui.html

### After Starting with Eureka:

1. **Check Eureka Dashboard:**
   Open: http://localhost:8761
   Should show: "ORDER" service with status UP

2. **Check service registration:**
   ```bash
   curl http://localhost:8080/api/discovery/services
   ```
   Should return: `["order"]`

3. **Check health:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   Should include:
   ```json
   {
     "serviceDiscovery": {
       "status": "UP",
       "details": {
         "services-discovered": 1
       }
     }
   }
   ```

---

## Common Issues

### Issue 1: Port 8761 already in use

**Error:**
```
Web server failed to start. Port 8761 was already in use.
```

**Solution:**
```bash
# Windows: Find process using port 8761
netstat -ano | findstr :8761

# Kill the process
taskkill /PID <PID> /F

# Or use different port
cd eureka-server
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8762"
```

### Issue 2: Port 8080 already in use

**Error:**
```
Web server failed to start. Port 8080 was already in use.
```

**Solution:**
```bash
# Windows: Find process using port 8080
netstat -ano | findstr :8080

# Kill the process
taskkill /PID <PID> /F

# Or use different port
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081,--spring.profiles.active=standalone"
```

### Issue 3: Eureka Server starts but Order Service doesn't register

**Symptoms:**
- Eureka Dashboard is empty
- Order Service logs show connection errors

**Solutions:**

1. **Wait longer** - Registration takes 5-10 seconds
2. **Check Eureka URL** - Should be `http://localhost:8761/eureka/`
3. **Check profile** - Should be using `eureka` profile
4. **Check firewall** - May be blocking localhost communication

**Debug command:**
```bash
# Check if Eureka is reachable
curl http://localhost:8761/eureka/apps
```

---

## Recommended Development Workflow

### For Daily Development (No Eureka Needed)

```bash
# Just start the app
start-order-service-1.bat

# Or
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=standalone"
```

**Benefits:**
- Faster startup
- No dependencies
- Simpler debugging

### For Testing Service Discovery

```bash
# Use the master script
start-all.bat

# Or manually:
# Terminal 1
cd eureka-server && mvn spring-boot:run

# Terminal 2 (wait 30 sec)
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=eureka"
```

---

## IDE Configuration

### IntelliJ IDEA

**Run Configuration 1: Standalone**
- Main Class: `com.example.order.OrderApplication`
- VM Options: `-Dspring.profiles.active=standalone`
- Working Directory: `D:\java\order-processing-java`

**Run Configuration 2: With Eureka**
- Main Class: `com.example.order.OrderApplication`
- VM Options: `-Dspring.profiles.active=eureka`
- Working Directory: `D:\java\order-processing-java`
- **Prerequisites:** Eureka Server must be running

**Run Configuration 3: Eureka Server**
- Main Class: `com.example.eureka.EurekaServerApplication`
- Working Directory: `D:\java\order-processing-java\eureka-server`

### Eclipse

**Run Configuration:**
- Right-click project → Run As → Run Configurations
- Java Application → New
- Main Class: `com.example.order.OrderApplication`
- Arguments tab → VM Arguments: `-Dspring.profiles.active=standalone`

---

## Files Updated

1. **Created:** `application-standalone.yml` - Profile for standalone mode
2. **Created:** `application-eureka.yml` - Profile for Eureka mode
3. **Updated:** `start-order-service-1.bat` - Now runs standalone by default
4. **Created:** `start-order-service-with-eureka.bat` - Runs with Eureka
5. **Created:** `start-all.bat` - Starts everything

---

## Quick Reference

| Mode | Command | Eureka Required? |
|------|---------|------------------|
| Standalone | `mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=standalone"` | ❌ No |
| With Eureka | `mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=eureka"` | ✅ Yes |
| Default | `mvn spring-boot:run` | ⚠️ Warns if missing |

---

## Summary

**Problem:** Connection refused to Eureka Server  
**Cause:** Eureka Server not running  
**Solution:** Choose one:
1. Run standalone (no Eureka): `start-order-service-1.bat`
2. Start Eureka first, then app: `start-all.bat`
3. Use standalone profile: `-Dspring.profiles.active=standalone`

**Recommendation for Development:** Use standalone mode unless specifically testing service discovery features.

