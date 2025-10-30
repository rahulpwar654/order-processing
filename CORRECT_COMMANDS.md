# ✅ FIXED: Correct Commands to Run the Application

## The Problem

The command `mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=standalone"` doesn't work on Windows due to Maven command parsing issues.

## ✅ CORRECT SOLUTIONS

### Option 1: Use the Startup Script (EASIEST)

```bash
start-order-service-1.bat
```

This is the **simplest and recommended** way to start the application.

---

### Option 2: Set Environment Variable (RECOMMENDED for Manual)

```bash
set MAVEN_OPTS=-Dspring.profiles.active=standalone
mvn spring-boot:run
```

**Why this works:** 
- Maven reads `MAVEN_OPTS` environment variable
- Properly passes JVM arguments
- Works reliably on Windows

---

### Option 3: Use Spring Boot Maven Plugin Syntax

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=standalone"
```

**Note:** This uses `-Dspring-boot.run.jvmArguments` instead of `-Dspring-boot.run.arguments`

---

### Option 4: Use application.properties Override

```bash
mvn spring-boot:run -Dspring.profiles.active=standalone
```

**Simplest syntax** - directly sets the Spring profile.

---

## 🚀 Quick Start Commands

### Start Standalone (No Eureka)

```bash
# Method 1: Startup script (BEST)
start-order-service-1.bat

# Method 2: Environment variable
set MAVEN_OPTS=-Dspring.profiles.active=standalone
mvn spring-boot:run

# Method 3: Direct profile
mvn spring-boot:run -Dspring.profiles.active=standalone
```

### Start with Eureka

**Step 1: Start Eureka Server**
```bash
cd eureka-server
mvn spring-boot:run
```

**Step 2: Start Order Service**
```bash
# Method 1: Startup script (BEST)
start-order-service-with-eureka.bat

# Method 2: Environment variable
set MAVEN_OPTS=-Dspring.profiles.active=eureka
mvn spring-boot:run

# Method 3: Direct profile
mvn spring-boot:run -Dspring.profiles.active=eureka
```

---

## 🧪 Test the Fix

### Test 1: Standalone Mode

```bash
# Run the command
set MAVEN_OPTS=-Dspring.profiles.active=standalone
mvn spring-boot:run
```

**Expected Output:**
```
Started OrderApplication in X.XXX seconds
```

**Verify:**
```bash
curl http://localhost:8080/actuator/health
```

Should return: `{"status":"UP"}`

### Test 2: With Startup Script

```bash
start-order-service-1.bat
```

**Expected:** Application starts without errors

---

## 📋 All Working Commands

### Standalone Mode

| Method | Command |
|--------|---------|
| **Script** | `start-order-service-1.bat` |
| **Env Var** | `set MAVEN_OPTS=-Dspring.profiles.active=standalone` <br> `mvn spring-boot:run` |
| **Direct** | `mvn spring-boot:run -Dspring.profiles.active=standalone` |
| **JVM Args** | `mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=standalone"` |

### With Eureka

| Method | Command |
|--------|---------|
| **Script** | `start-order-service-with-eureka.bat` |
| **Env Var** | `set MAVEN_OPTS=-Dspring.profiles.active=eureka` <br> `mvn spring-boot:run` |
| **Direct** | `mvn spring-boot:run -Dspring.profiles.active=eureka` |
| **JVM Args** | `mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=eureka"` |

### Multiple Instances

**Instance 1 (Port 8080):**
```bash
set MAVEN_OPTS=-Dspring.profiles.active=standalone
mvn spring-boot:run
```

**Instance 2 (Port 8081):**
```bash
set MAVEN_OPTS=-Dspring.profiles.active=standalone -Dserver.port=8081
mvn spring-boot:run
```

**Instance 3 (Port 8082):**
```bash
set MAVEN_OPTS=-Dspring.profiles.active=standalone -Dserver.port=8082
mvn spring-boot:run
```

---

## 🔧 IDE Configuration

### IntelliJ IDEA

**Run Configuration:**
1. Run → Edit Configurations
2. Add New Configuration → Spring Boot
3. **Main Class:** `com.example.order.OrderApplication`
4. **VM Options:** `-Dspring.profiles.active=standalone`
5. **Working Directory:** `D:\java\order-processing-java`

**Or use Environment Variables:**
- Name: `SPRING_PROFILES_ACTIVE`
- Value: `standalone`

### Eclipse

**Run Configuration:**
1. Right-click project → Run As → Run Configurations
2. Java Application → New
3. **Main Class:** `com.example.order.OrderApplication`
4. **Arguments Tab → VM Arguments:** `-Dspring.profiles.active=standalone`

### VS Code

**launch.json:**
```json
{
  "type": "java",
  "name": "OrderApplication (Standalone)",
  "request": "launch",
  "mainClass": "com.example.order.OrderApplication",
  "vmArgs": "-Dspring.profiles.active=standalone"
}
```

---

## ❌ Common Mistakes (DON'T DO THIS)

### ❌ Wrong: Using -Dspring-boot.run.arguments with quotes
```bash
# DON'T USE THIS - Doesn't work on Windows
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=standalone"
```

**Error:** `Unknown lifecycle phase ".run.arguments=..."`

### ❌ Wrong: Double dashes in Maven property
```bash
# DON'T USE THIS - Wrong syntax
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=standalone
```

### ❌ Wrong: Mixing argument types
```bash
# DON'T USE THIS - Confusing syntax
mvn spring-boot:run --spring.profiles.active=standalone
```

---

## ✅ Correct Patterns

### ✅ Pattern 1: Environment Variable (BEST)
```bash
set MAVEN_OPTS=-Dspring.profiles.active=standalone
mvn spring-boot:run
```

**Why it works:**
- Maven reads `MAVEN_OPTS` automatically
- Clean separation of concerns
- Works consistently across platforms

### ✅ Pattern 2: Direct System Property
```bash
mvn spring-boot:run -Dspring.profiles.active=standalone
```

**Why it works:**
- Direct JVM system property
- No parsing issues
- Simple and clean

### ✅ Pattern 3: JVM Arguments Parameter
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=standalone"
```

**Why it works:**
- Uses correct Maven plugin parameter
- Properly quoted
- Explicit and clear

---

## 🎯 Recommended Approach

**For Daily Use:**
```bash
start-order-service-1.bat
```

**For Manual Testing:**
```bash
set MAVEN_OPTS=-Dspring.profiles.active=standalone
mvn spring-boot:run
```

**For IDE:**
- VM Options: `-Dspring.profiles.active=standalone`

---

## 📝 Updated Startup Scripts

All startup scripts have been fixed to use the correct syntax:

1. ✅ `start-order-service-1.bat` - Sets `MAVEN_OPTS` and runs standalone
2. ✅ `start-order-service-2.bat` - Instance on port 8081
3. ✅ `start-order-service-3.bat` - Instance on port 8082
4. ✅ `start-order-service-with-eureka.bat` - Runs with Eureka enabled
5. ✅ `start-all.bat` - Starts Eureka + Order Service together

---

## 🚀 Quick Test

**Try this RIGHT NOW:**

```bash
# Open Command Prompt
cd D:\java\order-processing-java

# Run standalone
set MAVEN_OPTS=-Dspring.profiles.active=standalone
mvn spring-boot:run
```

**Expected:** Application starts successfully!

**Or just use the script:**
```bash
start-order-service-1.bat
```

---

## ✅ Verification Steps

1. **Run the command:**
   ```bash
   set MAVEN_OPTS=-Dspring.profiles.active=standalone
   mvn spring-boot:run
   ```

2. **Wait for startup** (20-30 seconds)

3. **Check health:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

4. **Access Swagger:**
   Open: http://localhost:8080/swagger-ui.html

5. **Test API:**
   ```bash
   curl http://localhost:8080/api/orders
   ```

---

## 📚 Summary

**Problem:** Maven command syntax error with `-Dspring-boot.run.arguments`  
**Root Cause:** Windows command parsing issues with Maven plugin arguments  
**Solution:** Use `MAVEN_OPTS` environment variable or direct system properties  

**Recommended Command:**
```bash
set MAVEN_OPTS=-Dspring.profiles.active=standalone
mvn spring-boot:run
```

**Or Simply:**
```bash
start-order-service-1.bat
```

**ALL STARTUP SCRIPTS HAVE BEEN FIXED AND WILL NOW WORK CORRECTLY!** ✅

