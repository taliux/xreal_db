@echo off
echo ========================================
echo Xreal FAQ Management System Startup
echo ========================================
echo.

echo Select environment:
echo 1. Development (No Elasticsearch required)
echo 2. Test (With Elasticsearch, will create/update tables)
echo 3. Production (No data modification)
echo.

set /p choice="Enter your choice (1-3): "

if "%choice%"=="1" (
    echo Starting in DEVELOPMENT mode...
    set SPRING_PROFILES_ACTIVE=dev
) else if "%choice%"=="2" (
    echo Starting in TEST mode...
    set SPRING_PROFILES_ACTIVE=default
) else if "%choice%"=="3" (
    echo Starting in PRODUCTION mode...
    set SPRING_PROFILES_ACTIVE=prod
) else (
    echo Invalid choice. Using default mode...
    set SPRING_PROFILES_ACTIVE=default
)

echo.
echo Environment: %SPRING_PROFILES_ACTIVE%
echo.

set JAVA_HOME=C:\Program Files\Java\jdk-22
set PATH=%JAVA_HOME%\bin;%PATH%

echo Starting application...
java -jar target\db-0.0.1-SNAPSHOT.jar --spring.profiles.active=%SPRING_PROFILES_ACTIVE%

pause