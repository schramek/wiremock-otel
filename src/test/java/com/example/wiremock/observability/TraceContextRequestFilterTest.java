package com.example.wiremock.observability;

import com.github.tomakehurst.wiremock.extension.requestfilter.ContinueAction;
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilterAction;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.Cookie;
import com.github.tomakehurst.wiremock.http.FormParameter;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceContextRequestFilterTest {
  private static final String TRACEPARENT = "00-11111111111111111111111111111111-2222222222222222-01";

  private final TraceContextRequestFilter filter = new TraceContextRequestFilter();

  @Test
  void keepsExistingW3cTraceparentHeader() {
    Request request = requestWithHeaders(Map.of("traceparent", TRACEPARENT));

    Request filteredRequest = filter(request);

    assertSame(request, filteredRequest);
    assertEquals(TRACEPARENT, filteredRequest.getHeader("traceparent"));
  }

  @Test
  void addsW3cTraceparentHeaderWhenMissing() {
    Request request = requestWithHeaders(Map.of());

    Request filteredRequest = filter(request);

    String traceparent = filteredRequest.getHeader("traceparent");
    assertNotNull(traceparent);
    assertTrue(traceparent.matches("00-[0-9a-f]{32}-[0-9a-f]{16}-01"));
  }

  private Request filter(Request request) {
    RequestFilterAction action = filter.filter(request, null);
    return ((ContinueAction) action).getRequest();
  }

  private static Request requestWithHeaders(Map<String, String> headers) {
    return new TestRequest(headers);
  }

  private static HttpHeaders httpHeaders(Map<String, String> headers) {
    List<HttpHeader> httpHeaders = headers.entrySet().stream()
        .map(entry -> new HttpHeader(entry.getKey(), entry.getValue()))
        .toList();
    return new HttpHeaders(httpHeaders);
  }

  private record TestRequest(Map<String, String> headers) implements Request {
    @Override
    public String getUrl() {
      return "/hello";
    }

    @Override
    public String getAbsoluteUrl() {
      return "http://localhost:8080/hello";
    }

    @Override
    public RequestMethod getMethod() {
      return RequestMethod.GET;
    }

    @Override
    public String getScheme() {
      return "http";
    }

    @Override
    public String getHost() {
      return "localhost";
    }

    @Override
    public int getPort() {
      return 8080;
    }

    @Override
    public String getClientIp() {
      return "127.0.0.1";
    }

    @Override
    public String getHeader(String key) {
      return headers.get(key);
    }

    @Override
    public HttpHeader header(String key) {
      String value = headers.get(key);
      return value == null ? HttpHeader.absent(key) : new HttpHeader(key, value);
    }

    @Override
    public ContentTypeHeader contentTypeHeader() {
      return ContentTypeHeader.absent();
    }

    @Override
    public HttpHeaders getHeaders() {
      return httpHeaders(headers);
    }

    @Override
    public boolean containsHeader(String key) {
      return headers.containsKey(key);
    }

    @Override
    public Set<String> getAllHeaderKeys() {
      return headers.keySet();
    }

    @Override
    public QueryParameter queryParameter(String key) {
      return QueryParameter.absent(key);
    }

    @Override
    public FormParameter formParameter(String key) {
      return FormParameter.absent(key);
    }

    @Override
    public Map<String, FormParameter> formParameters() {
      return Collections.emptyMap();
    }

    @Override
    public Map<String, Cookie> getCookies() {
      return Collections.emptyMap();
    }

    @Override
    public byte[] getBody() {
      return new byte[0];
    }

    @Override
    public String getBodyAsString() {
      return "";
    }

    @Override
    public String getBodyAsBase64() {
      return "";
    }

    @Override
    public boolean isMultipart() {
      return false;
    }

    @Override
    public Collection<Part> getParts() {
      return Collections.emptyList();
    }

    @Override
    public Part getPart(String name) {
      return null;
    }

    @Override
    public boolean isBrowserProxyRequest() {
      return false;
    }

    @Override
    public Optional<Request> getOriginalRequest() {
      return Optional.empty();
    }

    @Override
    public String getProtocol() {
      return "HTTP/1.1";
    }
  }
}
