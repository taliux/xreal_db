@echo off
echo ========================================
echo Xreal FAQ Management System Setup
echo ========================================
echo.

echo Step 1: Initializing MySQL database...
call init-database.bat
if %errorlevel% neq 0 (
    echo MySQL initialization failed!
    pause
    exit /b 1
)

echo.
echo Step 2: Initializing Elasticsearch index...
call init-elasticsearch.bat
if %errorlevel% neq 0 (
    echo Elasticsearch initialization failed!
    pause
    exit /b 1
)

echo.
echo Step 3: Building the application...
call mvn clean install -DskipTests
if %errorlevel% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Step 4: Starting the application...
echo ========================================
echo Application will be available at:
echo - API: http://localhost:8080
echo - Swagger UI: http://localhost:8080/swagger-ui.html
echo ========================================
echo.

call mvn spring-boot:run