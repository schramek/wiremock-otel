# wiremock-otel

## Trace Propagation

WireMock supports both W3C Trace Context and B3 multi-header propagation.

The OpenTelemetry Java agent is configured with:

```bash
OTEL_PROPAGATORS=tracecontext,baggage,b3multi
```

The WireMock extension preserves incoming propagation headers and adds the missing counterpart:

- Incoming `traceparent` is preserved and `X-B3-TraceId`, `X-B3-SpanId`, and `X-B3-Sampled` are added when absent.
- Incoming B3 multi headers are preserved and `traceparent` is added when absent.
- If no trace headers are present, both W3C and B3 multi headers are generated.