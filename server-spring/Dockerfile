# ============================================================
# Dockerfile — Tankzors Pro (Spring Boot)
# Multi-stage: build JAR với Maven, chạy với JRE nhẹ.
# ============================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom trước để cache dependencies (docker layer caching)
COPY server-spring/pom.xml ./pom.xml

# Download dependencies (layer này cache được nếu pom không đổi)
RUN mvn dependency:go-offline -B

# Copy source + resources
COPY server-spring/src ./src

# Build JAR (bỏ test trong Docker build cho nhanh)
RUN mvn package -DskipTests -B -q

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine

LABEL description="Tankzors Pro — Spring Boot + WebSocket + PostgreSQL"

WORKDIR /app

# Copy JAR từ stage build
COPY --from=builder /build/target/*.jar app.jar

# Expose port (application.yml: 8081)
EXPOSE 8081

# JVM flags:TimeString, container-friendly
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
