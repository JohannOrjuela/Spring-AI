#!/usr/bin/env bash
set -euo pipefail

# Simple integration test: POST to backend and expect non-empty answer
URL="http://localhost:8080/api/v1/chat"
PAYLOAD='{"question":"Integration test","sessionId":"it"}'

RESPONSE=$(curl -s -H "Content-Type: application/json" -d "$PAYLOAD" "$URL" || true)

if [ -z "$RESPONSE" ]; then
  echo "No response"
  exit 2
fi

echo "Response: $RESPONSE"
echo "$RESPONSE" | grep -q 'answer' || { echo "Missing answer field"; exit 3; }

echo "Integration smoke test passed"
