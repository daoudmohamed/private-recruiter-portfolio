# Multi-stage build for full-stack recruiter portfolio

# Stage 1: Frontend build
FROM node:22-bookworm-slim AS frontend-builder

WORKDIR /frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend ./
RUN npm run build

# Stage 2: Backend build
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Install tools needed by the Gradle wrapper
RUN apt-get update \
    && apt-get install -y --no-install-recommends bash curl unzip \
    && rm -rf /var/lib/apt/lists/*

# Copy Gradle wrapper and build files first for better caching
COPY gradlew ./
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Download dependencies (cached if no changes)
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src ./src
COPY documents ./documents
COPY --from=frontend-builder /frontend/dist ./src/main/resources/static

# Build the application
RUN ./gradlew bootJar --no-daemon -x test

# Stage 3: Runtime
FROM eclipse-temurin:25-jre

# Runtime utilities and non-root user
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && groupadd --system appgroup \
    && useradd --system --gid appgroup --create-home --home-dir /home/appuser appuser \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built jar
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy any additional resources (prompts, etc.)
COPY --from=builder /app/src/main/resources/prompts ./prompts
COPY --from=builder /app/documents ./documents

# Set ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD sh -c 'curl --fail --silent http://localhost:${MANAGEMENT_PORT:-8080}/actuator/health || exit 1'

# JVM settings for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
