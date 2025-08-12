@echo off
echo Starting Spring Boot Application with JDK 22...
set JAVA_HOME=C:\Program Files\Java\jdk-22
set PATH=%JAVA_HOME%\bin;%PATH%

cd /d C:\Users\Admin\Desktop\db

echo Using Java version:
java -version

echo.
echo Starting application...
mvn clean compile spring-boot:run

pause