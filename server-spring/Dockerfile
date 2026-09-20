# syntax=docker/dockerfile:1.7

# ============================================================
# Dockerfile — Tankzors Pro
#
# Stage 1: build LibGDX HTML5/GWT client bằng JDK 17.
# Stage 2: chép client vào Spring resources và build JAR bằng JDK 21.
# Stage 3: chạy ứng dụng bằng JRE 21 nhẹ.
#
# Nhờ vậy máy host chỉ cần Docker; không cần cài Java/Gradle/Maven.
# ============================================================

# ---- Stage 1: LibGDX HTML5 client ----
FROM gradle:8.10.2-jdk17 AS client-builder

WORKDIR /home/gradle/project

# Copy riêng game-client để cache Gradle độc lập với backend.
COPY --chown=gradle:gradle game-client/ ./

# Cache dependency Gradle qua các lần build, kể cả khi mạng bị gián đoạn.
RUN --mount=type=cache,id=tankzors-gradle,target=/home/gradle/.gradle,uid=1000,gid=1000 \
    gradle --no-daemon --console=plain html:dist

# ---- Stage 2: Spring Boot server ----
FROM maven:3.9-eclipse-temurin-21 AS server-builder

WORKDIR /build

COPY server-spring/pom.xml ./pom.xml

# Tải dependency theo pom trước để tận dụng Docker layer cache. Maven local
# repository dùng cache mount nên lần retry không phải tải lại từ đầu.
RUN --mount=type=cache,id=tankzors-maven,target=/root/.m2 \
    mvn -B -ntp -Dmaven.wagon.http.retryHandler.count=5 dependency:go-offline

COPY server-spring/src ./src

# Không đóng gói bundle client đã commit/stale. Luôn dùng output vừa build ở
# stage client-builder để Java source và JavaScript trong image đồng bộ.
RUN rm -rf ./src/main/resources/static/game/libgdx
COPY --from=client-builder /home/gradle/project/html/build/dist/ \
    ./src/main/resources/static/game/libgdx/

# Build JAR (test chạy riêng; Docker image production bỏ test để build nhanh).
RUN --mount=type=cache,id=tankzors-maven,target=/root/.m2 \
    mvn -B -ntp -Dmaven.wagon.http.retryHandler.count=5 -DskipTests package

# ---- Stage 3: Runtime ----
FROM eclipse-temurin:21-jre-alpine

LABEL description="Tankzors Pro — Spring Boot + WebSocket + PostgreSQL"

WORKDIR /app

# Copy JAR từ stage server build
COPY --from=server-builder /build/target/*.jar app.jar

# Expose port (application.yml: 8081)
EXPOSE 8081

# JVM flags:TimeString, container-friendly
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
