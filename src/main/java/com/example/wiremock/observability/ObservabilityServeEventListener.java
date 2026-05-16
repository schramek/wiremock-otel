package com.example.wiremock.observability;

import com.github.tomakehurst.wiremock.common.Urls;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ServeEventListener;
import com.github.tomakehurst.wiremock.http.LoggedResponse;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.UUID;

public class ObservabilityServeEventListener implements ServeEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObservabilityServeEventListener.class);
  private static final String[] MDC_KEYS = {
      "wiremock.request_id",
      "http.request.method",
      "url.path",
      "url.query",
      "http.response.status_code",
      "wiremock.was_matched",
      "wiremock.stub_id",
      "traceparent",
      "trace_id",
      "span_id"
  };

  @Override
  public void afterComplete(ServeEvent serveEvent, Parameters parameters) {
    Request request = serveEvent.getRequest();
    LoggedResponse response = serveEvent.getResponse();
    String traceparent = firstHeader(request, "traceparent").orElse(null);

    try {
      put("wiremock.request_id", stringValue(serveEvent.getId()));
      put("http.request.method", request.getMethod().getName());
      put("url.path", Urls.getPath(request.getUrl()));
      put("url.query", queryFromUrl(request.getUrl()).orElse(null));
      put("http.response.status_code", response == null ? null : String.valueOf(response.getStatus()));
      put("wiremock.was_matched", String.valueOf(serveEvent.getWasMatched()));
      put("wiremock.stub_id", stubId(serveEvent).orElse(null));
      put("traceparent", traceparent);
      put("trace_id", TraceContext.traceIdFromTraceparent(traceparent).orElse(null));
      put("span_id", TraceContext.spanIdFromTraceparent(traceparent).orElse(null));

      LOGGER.info("wiremock_request_completed");
    } finally {
      clearObservabilityMdc();
    }
  }

  @Override
  public String getName() {
    return "observability-serve-event-listener";
  }

  private static Optional<String> firstHeader(Request request, String name) {
    if (!request.containsHeader(name)) {
      return Optional.empty();
    }
    return Optional.ofNullable(request.getHeader(name));
  }

  private static Optional<String> queryFromUrl(String url) {
    int queryStart = url.indexOf('?');
    if (queryStart < 0 || queryStart == url.length() - 1) {
      return Optional.empty();
    }

    return Optional.of(url.substring(queryStart + 1));
  }

  private static Optional<String> stubId(ServeEvent serveEvent) {
    if (serveEvent.getStubMapping() == null) {
      return Optional.empty();
    }

    UUID id = serveEvent.getStubMapping().getId();
    return Optional.ofNullable(id).map(UUID::toString);
  }

  private static String stringValue(Object value) {
    return value == null ? null : value.toString();
  }

  private static void put(String key, String value) {
    if (value != null && !value.isBlank()) {
      MDC.put(key, value);
    }
  }

  private static void clearObservabilityMdc() {
    for (String key : MDC_KEYS) {
      MDC.remove(key);
    }
  }
}
