@echo off
REM Startup script for Order Service Instance 1
echo ====================================
echo Starting Order Service Instance 1
echo Port: 8080
echo Mode: Standalone (No Eureka)
echo API: http://localhost:8080
echo Swagger: http://localhost:8080/swagger-ui.html
echo ====================================
echo.
echo NOTE: Running in standalone mode.
echo To enable Eureka, start Eureka Server first,
echo then use: start-order-service-with-eureka.bat
echo ====================================
echo.

set MAVEN_OPTS=-Dspring.profiles.active=standalone
call mvn spring-boot:run

