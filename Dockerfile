# syntax=docker/dockerfile:1

# ---------- build stage ----------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Dependency layer: changes far less often than application source.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

# Application layer.
COPY src/ src/
RUN ./mvnw -B -ntp clean package -DskipTests \
    && mv target/*.jar /build/app.jar

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-jammy

# curl is required by the container HEALTHCHECK below.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Unprivileged runtime user (CIS Docker 4.1).
RUN groupadd --gid 1500 portal \
    && useradd --uid 1500 --gid portal --shell /usr/sbin/nologin --create-home portal

WORKDIR /app
COPY --from=builder --chown=portal:portal /build/app.jar /app/app.jar

USER portal

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://127.0.0.1:8080/actuator/health || exit 1

# Exec form: the JVM is PID 1 and receives SIGTERM directly for clean shutdown.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
