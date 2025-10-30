@echo off
REM Startup script for Order Service Instance 3
echo ====================================
echo Starting Order Service Instance 3
echo Port: 8082
echo Mode: Standalone
echo API: http://localhost:8082
echo ====================================

set MAVEN_OPTS=-Dspring.profiles.active=standalone -Dserver.port=8082
call mvn spring-boot:run

