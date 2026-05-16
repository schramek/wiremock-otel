package com.example.wiremock.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

final class TraceContext {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final HexFormat HEX = HexFormat.of();

  private TraceContext() {
  }

  static Optional<String> headerValueFromCurrentSpan() {
    SpanContext spanContext = Span.current().getSpanContext();
    if (!spanContext.isValid()) {
      return Optional.empty();
    }

    String sampledFlag = spanContext.isSampled() ? "01" : "00";
    return Optional.of("00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + sampledFlag);
  }

  static String generateTraceparent() {
    return "00-" + randomHex(16) + "-" + randomHex(8) + "-01";
  }

  static Optional<String> traceIdFromTraceparent(String traceparent) {
    if (traceparent == null) {
      return Optional.empty();
    }

    String[] parts = traceparent.trim().toLowerCase(Locale.ROOT).split("-");
    if (parts.length != 4 || parts[1].length() != 32) {
      return Optional.empty();
    }

    return Optional.of(parts[1]);
  }

  static Optional<String> spanIdFromTraceparent(String traceparent) {
    if (traceparent == null) {
      return Optional.empty();
    }

    String[] parts = traceparent.trim().toLowerCase(Locale.ROOT).split("-");
    if (parts.length != 4 || parts[2].length() != 16) {
      return Optional.empty();
    }

    return Optional.of(parts[2]);
  }

  private static String randomHex(int byteCount) {
    byte[] bytes = new byte[byteCount];
    RANDOM.nextBytes(bytes);
    return HEX.formatHex(bytes);
  }
}
