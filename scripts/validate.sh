#!/usr/bin/env bash
set -euo pipefail

WIREMOCK_URL="${WIREMOCK_URL:-http://localhost:8080}"
TRACEPARENT="${TRACEPARENT:-00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01}"

echo "Checking WireMock Admin mappings API"
curl --fail --silent --show-error "${WIREMOCK_URL}/__admin/mappings" >/dev/null

echo "Checking WireMock Admin requests API"
curl --fail --silent --show-error "${WIREMOCK_URL}/__admin/requests" >/dev/null

echo "Checking stub request with traceparent"
curl --fail --silent --show-error \
  -H "traceparent: ${TRACEPARENT}" \
  "${WIREMOCK_URL}/hello" >/dev/null

echo "Validation completed. Check container logs for wiremock_request_completed and OTel Collector output for spans."
