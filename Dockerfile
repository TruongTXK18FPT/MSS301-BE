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
# Core infrastructure services
COPY eureka-server ./eureka-server/
COPY gateway-service ./gateway-service/

# User and authentication services
COPY auth-service ./auth-service/
COPY profile-service ./profile-service/

# Content and document services
COPY content-service ./content-service/
COPY document-service ./document-service/

# Mindmap and related services
COPY mindmap-service ./mindmap-service/
COPY classroom-service ./classroom-service/

# Payment and premium services
COPY premium-service ./premium-service/
COPY payment-service ./payment-service/

# AI and processing services - rag-service must be before chatbot-service
COPY rag-service ./rag-service/
COPY retrieval-service ./retrieval-service/

# Other services
COPY notification-service ./notification-service/
COPY media-service ./media-service/

# Download dependencies separately for better layer caching
# This layer will be cached unless pom.xml changes
RUN mvn dependency:go-offline -pl ${SERVICE_NAME} -am || true

# Build the target service
RUN echo "Building ${SERVICE_NAME} with dependencies..." && \
    mvn clean install -pl ${SERVICE_NAME} -am -DskipTests -T 1 && \
    mvn spring-boot:repackage -pl ${SERVICE_NAME} -DskipTests -T 1

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
