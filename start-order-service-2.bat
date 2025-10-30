@echo off
REM Startup script for Order Service Instance 2
echo ====================================
echo Starting Order Service Instance 2
echo Port: 8081
echo Mode: Standalone
echo API: http://localhost:8081
echo ====================================

set MAVEN_OPTS=-Dspring.profiles.active=standalone -Dserver.port=8081
call mvn spring-boot:run > abcd.txt

