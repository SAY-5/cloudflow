# Multi-stage build for the ops-assistant. Built from the repo root so the Maven reactor
# can resolve platform-common. All module directories are copied so the reactor in
# the root pom resolves, but only this module is packaged.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY platform-common platform-common
COPY orders-service orders-service
COPY inventory-service inventory-service
COPY gateway gateway
COPY log-collector log-collector
COPY ops-assistant ops-assistant
RUN mvn -B -q -pl ops-assistant -am -DskipTests package

FROM eclipse-temurin:21-jre AS run
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 10001 cloudflow
COPY --from=build /workspace/ops-assistant/target/ops-assistant-*.jar app.jar
USER 10001
EXPOSE 8084
HEALTHCHECK --interval=10s --timeout=3s --start-period=70s --retries=15 \
  CMD curl -fsS http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
