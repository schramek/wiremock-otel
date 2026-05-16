package com.example.wiremock.observability;

import com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilterAction;
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestWrapper;
import com.github.tomakehurst.wiremock.extension.requestfilter.StubRequestFilterV2;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceContextRequestFilter implements StubRequestFilterV2 {
  private static final Logger LOGGER = LoggerFactory.getLogger(TraceContextRequestFilter.class);

  @Override
  public RequestFilterAction filter(Request request, ServeEvent serveEvent) {
    boolean hasTraceparent = hasHeader(request, "traceparent");
    boolean hasB3Multi = hasHeader(request, "X-B3-TraceId") && hasHeader(request, "X-B3-SpanId");

    if (hasTraceparent && hasB3Multi) {
      return RequestFilterAction.continueWith(request);
    }

    TraceContext.Headers headers = traceHeaders(request)
        .orElseGet(TraceContext::generateHeaders);

    RequestWrapper.Builder requestBuilder = RequestWrapper.create();
    if (!hasTraceparent) {
      requestBuilder.addHeader("traceparent", headers.traceparent());
    }
    if (!hasB3Multi) {
      requestBuilder
          .addHeader("X-B3-TraceId", headers.b3TraceId())
          .addHeader("X-B3-SpanId", headers.b3SpanId())
          .addHeader("X-B3-Sampled", headers.b3Sampled());
    }

    LOGGER.debug("Added missing trace propagation headers for WireMock stub/proxy request");
    return RequestFilterAction.continueWith(requestBuilder.wrap(request));
  }

  @Override
  public String getName() {
    return "trace-context-request-filter";
  }

  private static boolean hasHeader(Request request, String name) {
    return request.containsHeader(name);
  }

  private static java.util.Optional<TraceContext.Headers> traceHeaders(Request request) {
    return TraceContext.headersFromTraceparent(header(request, "traceparent"))
        .or(() -> TraceContext.headersFromB3(
            header(request, "X-B3-TraceId"),
            header(request, "X-B3-SpanId"),
            header(request, "X-B3-Sampled"),
            header(request, "X-B3-Flags")
        ))
        .or(TraceContext::headersFromCurrentSpan);
  }

  private static String header(Request request, String name) {
    return request.containsHeader(name) ? request.getHeader(name) : null;
  }
}
