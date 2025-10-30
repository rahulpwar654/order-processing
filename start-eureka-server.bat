@echo off
REM Startup script for Eureka Server
echo ====================================
echo Starting Eureka Server
echo Port: 8761
echo Dashboard: http://localhost:8761
echo ====================================

cd eureka-server
call mvn spring-boot:run

