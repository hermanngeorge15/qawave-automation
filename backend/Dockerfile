# ==============================================================================
# QAWave Backend Dockerfile
# Multi-stage build for optimized production image
# ==============================================================================
# Build: docker build -t qawave/backend:latest .
# Run:   docker run -p 8080:8080 qawave/backend:latest
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build the application
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install build dependencies
# hadolint ignore=DL3018
RUN apk add --no-cache bash

# Copy Gradle wrapper and build files first (better caching)
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./

# Make gradlew executable and download dependencies (cached layer)
RUN chmod +x gradlew && \
    ./gradlew dependencies --no-daemon --quiet

# Copy source code
COPY src src

# Build the application (skip tests for CI efficiency)
RUN ./gradlew bootJar --no-daemon -x test --quiet && \
    # Extract layers for faster startup (Spring Boot 2.3+)
    java -Djarmode=layertools -jar build/libs/*.jar extract --destination extracted

# ------------------------------------------------------------------------------
# Stage 2: Runtime image
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# Image metadata
LABEL org.opencontainers.image.title="QAWave Backend" \
      org.opencontainers.image.description="QAWave API Backend Service" \
      org.opencontainers.image.vendor="QAWave" \
      org.opencontainers.image.source="https://github.com/hermanngeorge15/qawave-automation" \
      org.opencontainers.image.licenses="MIT"

WORKDIR /app

# Install runtime dependencies and security updates
# hadolint ignore=DL3018
RUN apk add --no-cache \
    curl \
    dumb-init \
    && apk upgrade --no-cache \
    && rm -rf /var/cache/apk/*

# Create non-root user with specific UID/GID
RUN addgroup -g 1000 qawave && \
    adduser -u 1000 -G qawave -D -s /sbin/nologin qawave

# Copy layered application (for faster startup)
COPY --from=builder --chown=qawave:qawave /app/extracted/dependencies/ ./
COPY --from=builder --chown=qawave:qawave /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=qawave:qawave /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=qawave:qawave /app/extracted/application/ ./

# Switch to non-root user
USER qawave

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true"

# Use dumb-init as PID 1 for proper signal handling
ENTRYPOINT ["/usr/bin/dumb-init", "--"]

# Run the application with layered jars
CMD ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
