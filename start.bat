@echo off
REM License Validation Service - Start Script (Windows)
REM This script helps you quickly start the application

setlocal enabledelayedexpansion

echo ================================================
echo   License Validation Service
echo ================================================
echo.

REM Check if Java is installed
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not in PATH
    echo Please install Java 17 or higher
    pause
    exit /b 1
)

echo [OK] Java found
java -version

REM Check if Maven is installed
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Maven is not installed or not in PATH
    echo Please install Maven 3.6+
    pause
    exit /b 1
)

echo [OK] Maven found
mvn -version | findstr "Apache Maven"
echo.

REM Check if JWE_SECRET_KEY is set
if "%JWE_SECRET_KEY%"=="" (
    echo [WARNING] JWE_SECRET_KEY is not set
    echo Using default key (NOT SECURE for production^)
    echo To set a secure key (32 characters^):
    echo   set JWE_SECRET_KEY=your-32-character-secret-here!
    echo.
)

REM Create data directory if it doesn't exist
if not exist "data" mkdir data

REM Prompt for profile
echo Select profile:
echo   1^) Development (default, H2 console enabled^)
echo   2^) Production (H2 console disabled^)
echo.
set /p profile_choice="Enter choice [1-2] (default: 1): "

if "%profile_choice%"=="2" (
    set PROFILE=prod
    echo Starting with PRODUCTION profile...
) else (
    set PROFILE=default
    echo Starting with DEVELOPMENT profile...
)

echo.
echo Building project...
call mvn clean package -DskipTests

if %errorlevel% neq 0 (
    echo [ERROR] Build failed
    pause
    exit /b 1
)

echo.
echo Starting application...
echo ================================================
echo.

if "%PROFILE%"=="prod" (
    java -jar target\lib-validate-license-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
) else (
    java -jar target\lib-validate-license-0.0.1-SNAPSHOT.jar
)

pause
