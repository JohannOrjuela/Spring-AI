#!/usr/bin/env bash
set -euo pipefail

URL="http://localhost:8080/api/v1/chat"
VALID_PAYLOAD='{"question":"Hello, demo","sessionId":"demo","temperature":0.7,"topP":0.9,"topK":40,"numPredict":50,"seed":7}'
INVALID_PAYLOAD='{"question":"Must not reach the model","temperature":17,"numPredict":4096}'

echo "Running smoke test against $URL"
RESPONSE=$(curl -sS -H "Content-Type: application/json" -d "$VALID_PAYLOAD" "$URL" || true)

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

INVALID_RESULT=$(curl -sS -w $'\n%{http_code}' -H "Content-Type: application/json" -d "$INVALID_PAYLOAD" "$URL" || true)
INVALID_STATUS="${INVALID_RESULT##*$'\n'}"
INVALID_RESPONSE="${INVALID_RESULT%$'\n'*}"

if [ "$INVALID_STATUS" != "400" ]; then
  echo "Expected HTTP 400 for invalid sampling values, got $INVALID_STATUS"
  exit 5
fi

echo "$INVALID_RESPONSE" | jq -e '
  (.answer == "Request validation failed")
  and (.status == "error")
  and (.elapsedMs == 0)
  and (.promptTokens == null and .completionTokens == null and .totalTokens == null)
  and (.model == null and .finishReason == null and .tokensPerSecond == null)
  and ([.errors[].field] | sort == ["numPredict", "temperature"])
' >/dev/null || { echo "HTTP 400 response does not match the validation contract"; exit 6; }

echo "Smoke test passed"
