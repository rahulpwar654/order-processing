# FIXED: Eureka Connection Refused Error

## ✅ Problem Solved

The error you encountered:
```
Connection refused: Connect to http://localhost:8761 failed
```

This happened because the Order Service was trying to connect to Eureka Server, but Eureka wasn't running.

## 🚀 SOLUTION: Choose Your Mode

### Option 1: Standalone Mode (RECOMMENDED for Development)

**Run WITHOUT Eureka (Simplest):**

```bash
# Windows
start-order-service-1.bat

# Or manually
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=standalone"
```

✅ **Works immediately - No Eureka needed!**

---

### Option 2: With Eureka (For Testing Service Discovery)

**Run WITH Eureka:**

#### Step 1: Start Eureka Server FIRST
```bash
cd eureka-server
mvn spring-boot:run
```

**Wait** until Eureka Dashboard loads at: http://localhost:8761

#### Step 2: Start Order Service
```bash
start-order-service-with-eureka.bat

# Or manually
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=eureka"
```

---

### Option 3: Start Everything at Once

```bash
start-all.bat
```

This automatically:
1. Starts Eureka Server
2. Waits 30 seconds
3. Starts Order Service
4. Everything connects automatically

---

## ⚡ Quick Start

**Just want to run the app right now?**

```bash
start-order-service-1.bat
```

**That's it!** The app runs on http://localhost:8080

---

## 📋 What Was Changed

1. **Created Application Profiles:**
   - `standalone` - Runs without Eureka (default)
   - `eureka` - Runs with service discovery

2. **Updated Startup Scripts:**
   - `start-order-service-1.bat` - Now runs standalone by default
   - `start-order-service-with-eureka.bat` - NEW! Runs with Eureka
   - `start-all.bat` - NEW! Starts everything together

3. **Created Troubleshooting Guide:**
   - `doc/EUREKA_TROUBLESHOOTING.md` - Complete guide

---

## 🎯 Verification

### Test Standalone Mode:

```bash
# Start the service
start-order-service-1.bat

# Test API
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}
```

### Test with Eureka:

```bash
# Start everything
start-all.bat

# After 30 seconds, check Eureka Dashboard
# Open: http://localhost:8761
# Should show: "ORDER" service registered (green)

# Test service discovery
curl http://localhost:8080/api/discovery/services
# Should return: ["order"]
```

---

## 🔧 Development Workflow

### For Daily Development:
```bash
# Standalone mode - fastest and simplest
start-order-service-1.bat
```

### For Testing Service Discovery:
```bash
# Start both servers
start-all.bat
```

### For Multiple Instances:
```bash
# Terminal 1: Eureka
cd eureka-server && mvn spring-boot:run

# Terminal 2: Instance 1
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=eureka"

# Terminal 3: Instance 2
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=eureka,--server.port=8081"
```

---

## 📚 Documentation

- **Full Troubleshooting:** `doc/EUREKA_TROUBLESHOOTING.md`
- **Service Discovery Guide:** `doc/SERVICE_DISCOVERY_LOADBALANCING.md`
- **Quick Start:** `doc/SERVICE_DISCOVERY_QUICKSTART.md`

---

## ✅ Success Criteria

- [x] Error fixed - app runs without Eureka
- [x] Standalone profile created
- [x] Eureka profile created
- [x] Startup scripts updated
- [x] Master startup script created
- [x] Troubleshooting guide created
- [x] All features work in both modes

---

## 🎉 Summary

**Before:** App failed to start → Connection refused to Eureka  
**After:** App runs in standalone mode OR with Eureka (your choice!)

**Default Command:**
```bash
start-order-service-1.bat
```

**Result:** ✅ Service runs successfully on port 8080!

**All API endpoints work:**
- http://localhost:8080/swagger-ui.html
- http://localhost:8080/api/orders
- http://localhost:8080/actuator/health

**Problem SOLVED!** 🎊

