# Stage 1: Build the Spring Boot application
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Cache maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and package application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal, memory-safe JRE runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as a dedicated non-root user for security
RUN addgroup -S peakgroup && adduser -S peakuser -G peakgroup

# Copy compiled jar from builder stage
COPY --from=builder --chown=peakuser:peakgroup /build/target/*.jar app.jar

USER peakuser

# Render Free provides 512 MB RAM.
# -XX:MaxRAMPercentage=75.0 allocates ~384 MB heap, leaving ~128 MB headroom for JVM metaspace, threads, native memory, and OS.
# -XX:+ExitOnOutOfMemoryError immediately terminates container on OOM so the platform orchestrator can restart cleanly.
# Default PORT is 8080, overridden by Render's PORT environment variable.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC"
ENV PORT=8080

EXPOSE 8080

# Profile is controlled via SPRING_PROFILES_ACTIVE env var (e.g. prod or dev)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
