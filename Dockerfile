# Multi-stage Dockerfile for Spring Boot Services
# Optimized for 4 core 8GB RAM server
# Build with: docker build --build-arg SERVICE_NAME=auth-service -t mss301-auth-service .

FROM eclipse-temurin:21-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Build argument for service name
ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME}

# Copy parent POM first for better caching
COPY pom.xml ./

# Copy all services (required because parent POM declares all modules)
# If we only copy one service, Maven will fail because it can't find other modules
COPY eureka-server ./eureka-server/
COPY gateway-service ./gateway-service/
COPY auth-service ./auth-service/
COPY profile-service ./profile-service/
COPY content-service ./content-service/
COPY mindmap-service ./mindmap-service/
COPY premium-service ./premium-service/
COPY payment-service ./payment-service/
COPY notification-service ./notification-service/
COPY chatbot-service ./chatbot-service/
COPY classroom-service ./classroom-service/
COPY document-service ./document-service/
COPY rag-service ./rag-service/
COPY retrieval-service ./retrieval-service/
COPY media-service ./media-service/

# Download dependencies separately for better layer caching
# This layer will be cached unless pom.xml changes
RUN mvn dependency:go-offline -pl ${SERVICE_NAME} -am || true

# Build only the specified service
# Use single thread to reduce memory usage on 8GB RAM server
# -am (also-make) builds dependencies first (e.g., rag-service before chatbot-service)
# This ensures rag-service is built and installed before chatbot-service compiles
RUN mvn clean install -pl ${SERVICE_NAME} -am -DskipTests -T 1 spring-boot:repackage

# Runtime stage - use distroless for smaller image
FROM eclipse-temurin:21-jre-alpine

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Install wget for healthcheck
RUN apk add --no-cache wget

# Copy built JAR from builder stage
ARG SERVICE_NAME
# Copy the repackaged Spring Boot executable JAR
COPY --from=builder /app/${SERVICE_NAME}/target/${SERVICE_NAME}-*.jar app.jar

# Change ownership
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port (will be overridden by docker-compose)
EXPOSE 8080

# Health check - less aggressive for resource-constrained server
HEALTHCHECK --interval=45s --timeout=15s --start-period=90s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM optimizations for 8GB RAM server
# Allocate max 512MB per service (8GB / 16 services ≈ 512MB)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Xms256m", \
    "-Xmx512m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+UseStringDeduplication", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.jmx.enabled=false", \
    "-jar", "app.jar"]
