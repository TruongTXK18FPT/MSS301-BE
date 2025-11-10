# Multi-stage Dockerfile for Spring Boot Services
# Build with: docker build --build-arg SERVICE_NAME=auth-service -t mss301-auth-service .

FROM eclipse-temurin:21-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Copy parent POM
COPY pom.xml ./
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn

# Copy all service directories (for multi-module build)
COPY auth-service auth-service/
COPY payment-service payment-service/
COPY premium-service premium-service/
COPY mindmap-service mindmap-service/
COPY content-service content-service/
COPY chatbot-service chatbot-service/
COPY profile-service profile-service/
COPY notification-service notification-service/
COPY document-service document-service/
COPY retrieval-service retrieval-service/
COPY rag-service rag-service/
COPY classroom-service classroom-service/
COPY media-service media-service/
COPY eureka-server eureka-server/
COPY gateway-service gateway-service/

# Build argument for service name
ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME}

# Build only the specified service with Spring Boot repackage
RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests spring-boot:repackage

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

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

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
