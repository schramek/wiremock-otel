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
    return Optional.of(new Headers(
        "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId() + "-" + sampledFlag,
        spanContext.getTraceId(),
        spanContext.getSpanId(),
        spanContext.isSampled() ? "1" : "0"
    ));
  }

  static Headers generateHeaders() {
    String traceId = randomHex(16);
    String spanId = randomHex(8);
    return new Headers("00-" + traceId + "-" + spanId + "-01", traceId, spanId, "1");
  }

  static Optional<Headers> headersFromTraceparent(String traceparent) {
    Optional<String> traceId = traceIdFromTraceparent(traceparent);
    Optional<String> spanId = spanIdFromTraceparent(traceparent);
    if (traceId.isEmpty() || spanId.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(new Headers(traceparent.trim().toLowerCase(Locale.ROOT), traceId.get(), spanId.get(), sampledFromTraceparent(traceparent)));
  }

  static Optional<Headers> headersFromB3(String traceIdHeader, String spanIdHeader, String sampledHeader, String flagsHeader) {
    Optional<String> traceId = normalizeB3TraceId(traceIdHeader);
    Optional<String> spanId = normalizeB3SpanId(spanIdHeader);
    if (traceId.isEmpty() || spanId.isEmpty()) {
      return Optional.empty();
    }

    String sampled = sampledFromB3(sampledHeader, flagsHeader);
    String traceparent = "00-" + traceId.get() + "-" + spanId.get() + "-" + ("1".equals(sampled) ? "01" : "00");
    return Optional.of(new Headers(traceparent, traceId.get(), spanId.get(), sampled));
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

  static Optional<String> normalizeB3TraceId(String traceIdHeader) {
    if (traceIdHeader == null) {
      return Optional.empty();
    }

    String traceId = traceIdHeader.trim().toLowerCase(Locale.ROOT);
    if (LOWER_HEX_32.matcher(traceId).matches() && !isAllZeros(traceId)) {
      return Optional.of(traceId);
    }

    if (LOWER_HEX_16.matcher(traceId).matches() && !isAllZeros(traceId)) {
      return Optional.of("0000000000000000" + traceId);
    }

    return Optional.empty();
  }

  static Optional<String> normalizeB3SpanId(String spanIdHeader) {
    if (spanIdHeader == null) {
      return Optional.empty();
    }

    String spanId = spanIdHeader.trim().toLowerCase(Locale.ROOT);
    if (!LOWER_HEX_16.matcher(spanId).matches() || isAllZeros(spanId)) {
      return Optional.empty();
    }

    return Optional.of(spanId);
  }

  static String sampledFromB3(String sampledHeader, String flagsHeader) {
    if ("1".equals(flagsHeader)) {
      return "1";
    }

    if (sampledHeader == null) {
      return "0";
    }

    String sampled = sampledHeader.trim().toLowerCase(Locale.ROOT);
    return ("1".equals(sampled) || "true".equals(sampled)) ? "1" : "0";
  }

  private static String randomHex(int byteCount) {
    byte[] bytes = new byte[byteCount];
    RANDOM.nextBytes(bytes);
    return HEX.formatHex(bytes);
  }

  private static String sampledFromTraceparent(String traceparent) {
    String[] parts = traceparent.trim().toLowerCase(Locale.ROOT).split("-");
    if (!isValidTraceparent(parts)) {
      return "0";
    }

    int flags = Integer.parseInt(parts[3], 16);
    return (flags & 1) == 1 ? "1" : "0";
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

  record Headers(String traceparent, String b3TraceId, String b3SpanId, String b3Sampled) {
  }
}
