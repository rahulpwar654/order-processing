@echo off
REM Master startup script - Start Eureka and Order Service together
echo ====================================
echo Starting Complete System
echo ====================================
echo.
echo Step 1: Starting Eureka Server...
echo ====================================
start "Eureka Server" cmd /k "cd eureka-server && mvn spring-boot:run"
echo.
echo Waiting 30 seconds for Eureka Server to start...
timeout /t 30 /nobreak

echo.
echo Step 2: Starting Order Service...
echo ====================================
start "Order Service" cmd /k "set MAVEN_OPTS=-Dspring.profiles.active=eureka && mvn spring-boot:run"
echo.
echo ====================================
echo System Starting...
echo ====================================
echo.
echo Eureka Dashboard: http://localhost:8761
echo Order Service API: http://localhost:8080
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo.
echo Wait 10-15 seconds for Order Service to register with Eureka.
echo.
pause

