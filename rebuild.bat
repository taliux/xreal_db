@echo off
echo Cleaning and rebuilding project...
echo.

set JAVA_HOME=C:\Program Files\Java\jdk-22
set PATH=%JAVA_HOME%\bin;%PATH%

echo Step 1: Clean project
call mvnw.cmd clean

echo.
echo Step 2: Compile project
call mvnw.cmd compile

if %errorlevel% neq 0 (
    echo.
    echo Compilation failed! Trying with Java 11...
    set JAVA_HOME=C:\Program Files\Java\jdk-11
    call mvnw.cmd compile
)

echo.
echo Step 3: Package project
call mvnw.cmd package -DskipTests

echo.
echo Build complete!
pause