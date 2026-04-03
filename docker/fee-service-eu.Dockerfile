# ── Stage 1: Build ──────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY shared shared
COPY fee-service-eu fee-service-eu

RUN chmod +x gradlew && ./gradlew :fee-service-eu:bootJar -x test --no-daemon

# ── Stage 2: Runtime ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/fee-service-eu/build/libs/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
