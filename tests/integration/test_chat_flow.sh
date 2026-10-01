#!/usr/bin/env bash
set -euo pipefail

URL="http://localhost:8080/api/v1/chat"
SESSION_ID="integration-memory-$(date +%s)-$$"

check_turn() {
  local question="$1" response
  response=$(curl --fail-with-body -sS -H "Content-Type: application/json" \
    -d "$(jq -cn --arg question "$question" --arg sessionId "$SESSION_ID" \
      '{question:$question,sessionId:$sessionId,temperature:0,seed:7}')" "$URL")
  echo "$response" | jq -e '
    .status == "ok" and
    (.requestId | type == "string" and length > 0) and
    (.answer | type == "string" and test("\\S")) and
    (.elapsedMs | type == "number" and . >= 0) and
    has("promptTokens") and has("completionTokens") and has("totalTokens") and
    has("model") and has("finishReason") and has("tokensPerSecond") and has("errors")
  ' >/dev/null
}

command -v jq >/dev/null || { echo "jq is required"; exit 1; }
check_turn "Reten este marcador sintetico: ALFA-006."
check_turn "Continua la conversacion usando el contexto disponible."

echo "Two-turn integration contract passed"
