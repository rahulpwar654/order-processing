@echo off
REM Startup script for Order Service with Eureka enabled
echo ====================================
echo Starting Order Service with Eureka
echo Port: 8080
echo Mode: Service Discovery Enabled
echo API: http://localhost:8080
echo Swagger: http://localhost:8080/swagger-ui.html
echo Eureka Dashboard: http://localhost:8761
echo ====================================
echo.
echo PREREQUISITE: Eureka Server must be running!
echo Check: http://localhost:8761
echo.
echo Starting in 3 seconds...
timeout /t 3 /nobreak >nul
echo.

set MAVEN_OPTS=-Dspring.profiles.active=eureka
call mvn spring-boot:run

