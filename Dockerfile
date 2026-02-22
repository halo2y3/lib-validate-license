# Multi-stage Dockerfile for Spring Boot License Validation Library
# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (layer caching optimization)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built JAR from builder stage
COPY --from=builder /app/target/lib-validate-license-0.0.1-SNAPSHOT.jar app.jar

# Create directory for H2 database with proper permissions
RUN mkdir -p /app/data && chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose the application port (default 8199)
EXPOSE 8199

# Environment variables with defaults
ENV SERVER_PORT=8199 \
    SPRING_PROFILES_ACTIVE=prod \
    JWE_SECRET_KEY="" \
    JWE_EXPIRATION_SECONDS=3600 \
    JWE_ISSUER=lib-validate-license \
    H2_CONSOLE_ENABLED=false

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
