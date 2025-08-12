@echo off
echo Creating MySQL database and tables...

mysql -u xreal -pXreal2025 < src\main\resources\schema.sql

if %errorlevel% == 0 (
    echo MySQL database and tables created successfully!
) else (
    echo Failed to create MySQL database and tables.
    exit /b 1
)

echo.
echo Database initialization completed!
echo You can now run the Spring Boot application.
pause