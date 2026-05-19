package com.example.wiremock.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

final class TraceContext {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final HexFormat HEX = HexFormat.of();
  private static final Pattern LOWER_HEX_2 = Pattern.compile("[0-9a-f]{2}");
  private static final Pattern LOWER_HEX_16 = Pattern.compile("[0-9a-f]{16}");
  private static final Pattern LOWER_HEX_32 = Pattern.compile("[0-9a-f]{32}");

  private TraceContext() {
  }

  static Optional<Headers> headersFromCurrentSpan() {
    SpanContext spanContext = Span.current().getSpanContext();
    if (!spanContext.isValid()) {
      return Optional.empty();
    }

    String sampledFlag = spanContext.isSampled() ? "01" : "00";
    return Optional.of(new Headers("00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + sampledFlag));
  }

  static Headers generateHeaders() {
    String traceId = randomHex(16);
    String spanId = randomHex(8);
    return new Headers("00-" + traceId + "-" + spanId + "-01");
  }

  static Optional<Headers> headersFromTraceparent(String traceparent) {
    Optional<String> traceId = traceIdFromTraceparent(traceparent);
    Optional<String> spanId = spanIdFromTraceparent(traceparent);
    if (traceId.isEmpty() || spanId.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(new Headers(traceparent.trim().toLowerCase(Locale.ROOT)));
  }

  static Optional<String> traceIdFromTraceparent(String traceparent) {
    if (traceparent == null) {
      return Optional.empty();
    }

    String[] parts = traceparent.trim().toLowerCase(Locale.ROOT).split("-");
    if (!isValidTraceparent(parts) || isAllZeros(parts[1])) {
      return Optional.empty();
    }

    return Optional.of(parts[1]);
  }

  static Optional<String> spanIdFromTraceparent(String traceparent) {
    if (traceparent == null) {
      return Optional.empty();
    }

    String[] parts = traceparent.trim().toLowerCase(Locale.ROOT).split("-");
    if (!isValidTraceparent(parts) || isAllZeros(parts[2])) {
      return Optional.empty();
    }

    return Optional.of(parts[2]);
  }

  private static String randomHex(int byteCount) {
    byte[] bytes = new byte[byteCount];
    RANDOM.nextBytes(bytes);
    return HEX.formatHex(bytes);
  }

  private static boolean isAllZeros(String value) {
    return value.chars().allMatch(character -> character == '0');
  }

  private static boolean isValidTraceparent(String[] parts) {
    return parts.length == 4
        && "00".equals(parts[0])
        && LOWER_HEX_32.matcher(parts[1]).matches()
        && LOWER_HEX_16.matcher(parts[2]).matches()
        && LOWER_HEX_2.matcher(parts[3]).matches();
  }

  record Headers(String traceparent) {
  }
}
