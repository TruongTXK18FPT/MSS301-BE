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

# Special handling for services that depend on other modules (e.g., chatbot-service depends on rag-service)
# Build and install all module dependencies first to ensure they're in local Maven repository
# This is critical for inter-module dependencies
# CRITICAL: rag-service needs to be built as a regular JAR (not executable) for use as dependency
RUN if [ "${SERVICE_NAME}" = "chatbot-service" ]; then \
        echo "==========================================="; \
        echo "Building rag-service dependency first..."; \
        echo "==========================================="; \
        # Step 1: Compile rag-service \
        echo "[1/3] Compiling rag-service..."; \
        mvn compile -pl rag-service -DskipTests -T 1; \
        # Step 2: Package as regular JAR (skip spring-boot repackage) \
        echo "[2/3] Packaging rag-service as dependency JAR..."; \
        mvn jar:jar -pl rag-service -DskipTests -T 1 || \
        mvn package -pl rag-service -DskipTests -T 1 -Dspring-boot.repackage.skip=true; \
        # Step 3: Install to local Maven repository \
        echo "[3/3] Installing rag-service to local repository..."; \
        mvn install:install-file \
            -Dfile=rag-service/target/rag-service-0.0.1-SNAPSHOT.jar \
            -DgroupId=com.MSS301 \
            -DartifactId=rag-service \
            -Dversion=0.0.1-SNAPSHOT \
            -Dpackaging=jar \
            -DpomFile=rag-service/pom.xml || \
        (echo "Fallback: Using standard install without repackage..." && \
         mvn install -pl rag-service -DskipTests -T 1 -Dspring-boot.repackage.skip=true); \
        # Verify installation \
        echo "Verifying rag-service installation..."; \
        if [ -f ~/.m2/repository/com/MSS301/rag-service/0.0.1-SNAPSHOT/rag-service-0.0.1-SNAPSHOT.jar ]; then \
            echo "✓ rag-service JAR found in local repository"; \
            ls -lh ~/.m2/repository/com/MSS301/rag-service/0.0.1-SNAPSHOT/rag-service-0.0.1-SNAPSHOT.jar; \
        else \
            echo "⚠ Warning: rag-service JAR not found, checking directory..."; \
            ls -la ~/.m2/repository/com/MSS301/rag-service/0.0.1-SNAPSHOT/ 2>/dev/null || \
            find ~/.m2/repository -name "*rag-service*" -type f 2>/dev/null | head -3 || echo "Not found"; \
        fi; \
        echo "✓ rag-service installation completed"; \
    fi

# Build only the specified service
# Use single thread to reduce memory usage on 8GB RAM server
# For chatbot-service: Don't use -am to avoid rebuilding rag-service (already installed above)
# For other services: Use -am to build dependencies
RUN if [ "${SERVICE_NAME}" = "chatbot-service" ]; then \
        echo "Building chatbot-service (rag-service already installed)..."; \
        mvn compile -pl ${SERVICE_NAME} -DskipTests -T 1 && \
        mvn package -pl ${SERVICE_NAME} -DskipTests -T 1 && \
        mvn spring-boot:repackage -pl ${SERVICE_NAME} -DskipTests -T 1; \
    else \
        echo "Building ${SERVICE_NAME} with dependencies..."; \
        mvn install -pl ${SERVICE_NAME} -am -DskipTests -T 1 && \
        mvn spring-boot:repackage -pl ${SERVICE_NAME} -DskipTests -T 1; \
    fi

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
