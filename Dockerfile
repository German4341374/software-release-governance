# syntax=docker/dockerfile:1.12
FROM eclipse-temurin:26-jdk-alpine-3.23 AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress clean package

FROM eclipse-temurin:26-jre-alpine-3.23 AS runtime
RUN addgroup -S governance --gid 10001 \
    && adduser -S governance -G governance -u 10001 \
    && mkdir -p /opt/governance \
    && chown governance:governance /opt/governance

WORKDIR /opt/governance
COPY --from=build --chown=governance:governance /workspace/target/software-release-governance-*.jar app.jar

USER 10001:10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "/opt/governance/app.jar"]
