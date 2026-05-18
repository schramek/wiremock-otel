ARG WIREMOCK_DOCKER_TAG=3.13.2-3

FROM maven:3.9-eclipse-temurin-17 AS extension-build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package

FROM wiremock/wiremock:${WIREMOCK_DOCKER_TAG}

USER root

COPY --from=extension-build /workspace/target/otel/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
COPY --from=extension-build /workspace/target/wiremock-observability-extension.jar /var/wiremock/extensions/wiremock-observability-extension.jar
COPY --from=extension-build /workspace/target/classes/wiremock /home/wiremock

RUN chmod 0644 /otel/opentelemetry-javaagent.jar \
    /var/wiremock/extensions/wiremock-observability-extension.jar

ENV JAVA_TOOL_OPTIONS="-javaagent:/otel/opentelemetry-javaagent.jar"
ENV OTEL_SERVICE_NAME="wiremock"
ENV OTEL_TRACES_EXPORTER="otlp"
ENV OTEL_METRICS_EXPORTER="none"
ENV OTEL_LOGS_EXPORTER="none"
ENV OTEL_PROPAGATORS="tracecontext,baggage,b3multi"
ENV OTEL_EXPORTER_OTLP_ENDPOINT="http://otel-collector:4317"

USER wiremock

ENTRYPOINT ["/docker-entrypoint.sh", "--extensions", "com.example.wiremock.observability.ObservabilityServeEventListener,com.example.wiremock.observability.TraceContextRequestFilter"]
