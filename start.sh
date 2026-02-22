#!/bin/bash

# License Validation Service - Start Script
# This script helps you quickly start the application

set -e

echo "================================================"
echo "  License Validation Service"
echo "================================================"
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed or not in PATH"
    echo "Please install Java 17 or higher"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Error: Java 17 or higher is required"
    echo "Current version: $(java -version 2>&1 | head -n 1)"
    exit 1
fi

echo "✅ Java version: $(java -version 2>&1 | head -n 1)"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven is not installed or not in PATH"
    echo "Please install Maven 3.6+"
    exit 1
fi

echo "✅ Maven version: $(mvn -version | head -n 1)"
echo ""

# Check if JWE_SECRET_KEY is set
if [ -z "$JWE_SECRET_KEY" ]; then
    echo "⚠️  Warning: JWE_SECRET_KEY is not set"
    echo "Using default key (NOT SECURE for production)"
    echo "To set a secure key (32 characters):"
    echo "  export JWE_SECRET_KEY='your-32-character-secret-here!'"
    echo ""
fi

# Create data directory if it doesn't exist
mkdir -p data

# Prompt for profile
echo "Select profile:"
echo "  1) Development (default, H2 console enabled)"
echo "  2) Production (H2 console disabled)"
echo ""
read -p "Enter choice [1-2] (default: 1): " profile_choice

case "$profile_choice" in
    2)
        PROFILE="prod"
        echo "Starting with PRODUCTION profile..."
        ;;
    *)
        PROFILE="default"
        echo "Starting with DEVELOPMENT profile..."
        ;;
esac

echo ""
echo "Building project..."
mvn clean package -DskipTests

echo ""
echo "Starting application..."
echo "================================================"
echo ""

if [ "$PROFILE" = "prod" ]; then
    java -jar target/lib-validate-license-0.0.1-SNAPSHOT.jar \
        --spring.profiles.active=prod
else
    java -jar target/lib-validate-license-0.0.1-SNAPSHOT.jar
fi
