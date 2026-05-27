# syntax=docker/dockerfile:1

ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-alpine

FROM ${MAVEN_IMAGE} AS build

WORKDIR /workspace

COPY . .

RUN mvn --batch-mode --no-transfer-progress -pl apps/backend -am package -DskipTests

FROM ${RUNTIME_IMAGE}

RUN apk add --no-cache curl tini \
    && addgroup -S sihsalus \
    && adduser -S -G sihsalus -u 1001 sihsalus

WORKDIR /opt/sihsalus

ENV OPENMRS_APPLICATION_DATA_DIRECTORY=/openmrs/data \
    SIHSALUS_INITIALIZER_SOURCE_ROOT=/opt/sihsalus/reference-sources/sihsalus-content \
    SERVER_PORT=8080 \
    JAVA_OPTS=""

RUN mkdir -p /openmrs/data /opt/sihsalus/reference-sources \
    && chown -R sihsalus:sihsalus /openmrs /opt/sihsalus

COPY --from=build --chown=sihsalus:sihsalus /workspace/apps/backend/target/sihsalus-core-boot-*.jar /opt/sihsalus/app.jar
COPY --chown=sihsalus:sihsalus .dev/reference-sources/sihsalus-content /opt/sihsalus/reference-sources/sihsalus-content
COPY --chown=sihsalus:sihsalus docker/entrypoint.sh /opt/sihsalus/entrypoint.sh

RUN chmod 0555 /opt/sihsalus/entrypoint.sh

USER sihsalus

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=2m --retries=12 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["/sbin/tini", "--", "/opt/sihsalus/entrypoint.sh"]
