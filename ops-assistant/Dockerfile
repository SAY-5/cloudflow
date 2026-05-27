# Multi-stage build for the ops-assistant service. Built from the repo root so the
# Maven reactor can resolve platform-common.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY platform-common platform-common
COPY ops-assistant ops-assistant
RUN mvn -B -q -pl ops-assistant -am -DskipTests package

FROM eclipse-temurin:21-jre AS run
WORKDIR /app
RUN useradd -r -u 10001 cloudflow
COPY --from=build /workspace/ops-assistant/target/ops-assistant-*.jar app.jar
USER 10001
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
