# ============================================================
# Multi-stage Dockerfile for zhi-pm-server
# Build: Maven 3.9 + JDK 21 | Runtime: JRE 21
# ============================================================

# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# Copy pom files first for dependency caching
COPY pom.xml ./
COPY zhi-pm-core/pom.xml zhi-pm-core/pom.xml
COPY zhi-pm-spring-boot-autoconfigure/pom.xml zhi-pm-spring-boot-autoconfigure/pom.xml
COPY zhi-pm-spring-boot-starter/pom.xml zhi-pm-spring-boot-starter/pom.xml
COPY zhi-pm-broker-redis/pom.xml zhi-pm-broker-redis/pom.xml
COPY zhi-pm-broker-kafka/pom.xml zhi-pm-broker-kafka/pom.xml
COPY zhi-pm-danmaku/pom.xml zhi-pm-danmaku/pom.xml
COPY zhi-pm-chat/pom.xml zhi-pm-chat/pom.xml
COPY zhi-pm-observability/pom.xml zhi-pm-observability/pom.xml
COPY zhi-pm-admin-api/pom.xml zhi-pm-admin-api/pom.xml
COPY zhi-pm-admin-ui/pom.xml zhi-pm-admin-ui/pom.xml
COPY zhi-pm-server/pom.xml zhi-pm-server/pom.xml
COPY samples/pom.xml samples/pom.xml
COPY samples/basic/pom.xml samples/basic/pom.xml
COPY samples/basic/sample-basic-echo/pom.xml samples/basic/sample-basic-echo/pom.xml
COPY samples/business/pom.xml samples/business/pom.xml
COPY samples/realtime/pom.xml samples/realtime/pom.xml
COPY samples/operations/pom.xml samples/operations/pom.xml

# Download dependencies (cached unless pom files change)
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY . .

# Build the server module (skip tests for Docker build)
RUN mvn package -pl zhi-pm-server -am -DskipTests -B

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="io.github.zhi"
LABEL description="Zhi Push Message - Reactive WebSocket Gateway"

# Create non-root user
RUN addgroup -S zhipm && adduser -S zhipm -G zhipm

WORKDIR /app

# Copy the built jar
COPY --from=build /workspace/zhi-pm-server/target/zhi-pm-server-*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R zhipm:zhipm /app

USER zhipm

# Expose default port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
