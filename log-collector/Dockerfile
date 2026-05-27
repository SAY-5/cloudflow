# Multi-stage build for the log-collector service. Built from the repo root so the
# Maven reactor can resolve platform-common.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY platform-common platform-common
COPY log-collector log-collector
RUN mvn -B -q -pl log-collector -am -DskipTests package

FROM eclipse-temurin:21-jre AS run
WORKDIR /app
RUN useradd -r -u 10001 cloudflow
COPY --from=build /workspace/log-collector/target/log-collector-*.jar app.jar
USER 10001
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
