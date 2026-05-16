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
    if (hasHeader(request, "traceparent")) {
      return RequestFilterAction.continueWith(request);
    }

    String traceparent = TraceContext.headerValueFromCurrentSpan()
        .orElseGet(TraceContext::generateTraceparent);

    Request wrappedRequest = RequestWrapper.create()
        .addHeader("traceparent", traceparent)
        .wrap(request);

    LOGGER.debug("Added traceparent header for WireMock stub/proxy request");
    return RequestFilterAction.continueWith(wrappedRequest);
  }

  @Override
  public String getName() {
    return "trace-context-request-filter";
  }

  private static boolean hasHeader(Request request, String name) {
    return request.containsHeader(name);
  }
}
