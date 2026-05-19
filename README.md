# wiremock-otel

## Trace Propagation

WireMock supports W3C Trace Context propagation.

The OpenTelemetry Java agent is configured with:

```bash
OTEL_PROPAGATORS=tracecontext,baggage
```

The agent JAR is managed by Maven via `opentelemetry.javaagent.version` in `pom.xml` and copied into the Docker image from `target/otel/opentelemetry-javaagent.jar`.

The WireMock extension preserves incoming W3C propagation headers and adds a missing `traceparent` header when absent.
