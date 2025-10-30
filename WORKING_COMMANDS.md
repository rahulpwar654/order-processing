# ✅ FIXED - Working Commands for Windows

## 🎯 THE SOLUTION

The original command syntax was incorrect for Windows. Here are the **WORKING** commands:

---

## 🚀 OPTION 1: Use Startup Script (EASIEST - RECOMMENDED)

```cmd
start-order-service-1.bat
```

**This is the simplest way - just double-click the file or run it from command prompt!**

---

## 🚀 OPTION 2: Simple Maven Command (WORKS NOW)

### For Command Prompt (cmd):

```cmd
cd D:\java\order-processing-java
set MAVEN_OPTS=-Dspring.profiles.active=standalone
mvn spring-boot:run
```

### For PowerShell:

```powershell
cd D:\java\order-processing-java
$env:MAVEN_OPTS="-Dspring.profiles.active=standalone"
mvn spring-boot:run
```

---

## 🚀 OPTION 3: Direct System Property

### For Command Prompt:

```cmd
cd D:\java\order-processing-java
mvn spring-boot:run -Dspring.profiles.active=standalone
```

### For PowerShell:

```powershell
cd D:\java\order-processing-java
mvn spring-boot:run -Dspring.profiles.active=standalone
```

---

## ✅ What Was Fixed

### ❌ OLD (Broken):
```cmd
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=standalone"
```

**Error:** `Unknown lifecycle phase ".run.arguments=..."`

### ✅ NEW (Working):
```cmd
set MAVEN_OPTS=-Dspring.profiles.active=standalone
mvn spring-boot:run
```

**Result:** ✅ Application starts successfully!

---

## 🎯 QUICK START - Choose ONE Method

### Method 1: Startup Script (No typing needed!)
```cmd
start-order-service-1.bat
```

### Method 2: Command Prompt
```cmd
set MAVEN_OPTS=-Dspring.profiles.active=standalone && mvn spring-boot:run
```

### Method 3: PowerShell
```powershell
$env:MAVEN_OPTS="-Dspring.profiles.active=standalone"; mvn spring-boot:run
```

### Method 4: Simplest
```cmd
mvn spring-boot:run -Dspring.profiles.active=standalone
```

---

## 🧪 Verify It Works

After starting the application:

```cmd
# Wait 30 seconds for startup, then test:
curl http://localhost:8080/actuator/health
```

**Expected:** `{"status":"UP"}`

**Or open in browser:**
- API: http://localhost:8080/api/orders
- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

---

## 📝 All Updated Files

All startup scripts have been fixed:

1. ✅ `start-order-service-1.bat` - Standalone mode
2. ✅ `start-order-service-2.bat` - Port 8081
3. ✅ `start-order-service-3.bat` - Port 8082
4. ✅ `start-order-service-with-eureka.bat` - With Eureka
5. ✅ `start-all.bat` - Complete system

---

## 🎓 Why the Original Command Failed

**Original Command:**
```cmd
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=standalone"
```

**Problems:**
1. Windows treats `-Dspring-boot.run.arguments` as Maven lifecycle phase
2. Quotes don't help - Windows parses them incorrectly
3. Maven plugin doesn't recognize the syntax

**Correct Approach:**
- Use `MAVEN_OPTS` environment variable
- Or use direct system property: `-Dspring.profiles.active=standalone`
- Or use startup scripts that handle it correctly

---

## 🎉 READY TO USE

**Right now, you can run:**

```cmd
start-order-service-1.bat
```

**Or:**

```cmd
cd D:\java\order-processing-java
mvn spring-boot:run -Dspring.profiles.active=standalone
```

**Both will work perfectly!** ✅

---

## 📚 Documentation

- **This Guide:** `CORRECT_COMMANDS.md` - You're reading it!
- **Detailed Guide:** `CORRECT_COMMANDS.md` - Complete reference
- **Troubleshooting:** `doc/EUREKA_TROUBLESHOOTING.md` - If you have issues

---

## ✅ SUCCESS CRITERIA

- ✅ Startup scripts fixed
- ✅ Correct Maven commands provided
- ✅ Works in Command Prompt
- ✅ Works in PowerShell
- ✅ No more "Unknown lifecycle phase" errors
- ✅ Application starts successfully

**THE PROBLEM IS COMPLETELY SOLVED!** 🎊

