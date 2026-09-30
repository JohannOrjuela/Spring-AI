#!/usr/bin/env bash
set -euo pipefail

URL="http://localhost:8080/api/v1/chat"
PAYLOAD='{"question":"Hello, demo","sessionId":"demo"}'

echo "Running smoke test against $URL"
RESPONSE=$(curl -s -H "Content-Type: application/json" -d "$PAYLOAD" "$URL" || true)

if [ -z "$RESPONSE" ]; then
  echo "No response from backend"
  exit 2
fi

command -v jq >/dev/null 2>&1 || { echo "jq is required to validate the chat response"; exit 3; }

echo "$RESPONSE" | jq -e '
  (.answer | type == "string" and length > 0)
  and (.status == "ok")
  and (has("requestId") and has("elapsedMs"))
  and (has("promptTokens") and has("completionTokens") and has("totalTokens")
    and has("model") and has("finishReason") and has("tokensPerSecond"))
  and ([.promptTokens, .completionTokens, .totalTokens, .model, .finishReason, .tokensPerSecond]
    | all(. == null or (. | type == "number" or type == "string")))
' >/dev/null || { echo "Response does not match the token metrics contract"; exit 4; }

echo "Smoke test passed"
