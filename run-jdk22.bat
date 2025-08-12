@echo off
echo =====================================
echo Xreal FAQ Management System
echo Using JDK 22
echo =====================================
echo.

REM Set JDK 22 environment
set JAVA_HOME=C:\Program Files\Java\jdk-22
set PATH=%JAVA_HOME%\bin;%PATH%

REM Clear problematic environment variables
set _JAVA_OPTIONS=

echo Checking Java version...
"%JAVA_HOME%\bin\java" -version
echo.

echo =====================================
echo Please use one of these methods:
echo =====================================
echo.
echo METHOD 1: Use IntelliJ IDEA (Recommended)
echo ------------------------------------------
echo 1. Open IntelliJ IDEA
echo 2. File -^> Open -^> Select this folder: C:\Users\Admin\Desktop\db
echo 3. Wait for Maven to sync
echo 4. Go to File -^> Project Structure
echo 5. Set Project SDK to: C:\Program Files\Java\jdk-22
echo 6. Set Project language level to: 22
echo 7. Apply and OK
echo 8. Find DbApplication.java in src/main/java/com/xreal/db/
echo 9. Right-click and select "Run DbApplication"
echo.
echo METHOD 2: Command Line with Maven
echo ------------------------------------------
echo If you have Maven installed separately:
echo.
echo cd C:\Users\Admin\Desktop\db
echo set JAVA_HOME=C:\Program Files\Java\jdk-22
echo mvn clean compile
echo mvn spring-boot:run
echo.
echo METHOD 3: Direct JAR execution
echo ------------------------------------------
echo After building in IDE:
echo "%JAVA_HOME%\bin\java" -jar target\db-0.0.1-SNAPSHOT.jar
echo.
echo =====================================
echo System Requirements:
echo =====================================
echo [√] JDK 22 installed at: C:\Program Files\Java\jdk-22
echo [√] MySQL running on port 3306
echo [√] Elasticsearch running on port 9200
echo [√] Database: xreal_tech_faq
echo [√] ES Index: xreal_tech_faq
echo.
echo =====================================
echo API Endpoints (after starting):
echo =====================================
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo FAQ API: http://localhost:8080/faqs
echo Tag API: http://localhost:8080/tags
echo.
pause