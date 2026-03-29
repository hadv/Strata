# Stage 1: Build
FROM gradle:8.12-jdk21 AS build
WORKDIR /app

# Copy build files first for better layer caching
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY shared/build.gradle.kts shared/
COPY command/build.gradle.kts command/

# Download dependencies (cached layer)
RUN gradle :command:dependencies --no-daemon || true

# Copy source code
COPY shared/src shared/src
COPY command/src command/src

# Build the application
RUN gradle :command:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S strata && adduser -S strata -G strata
USER strata

COPY --from=build /app/command/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
