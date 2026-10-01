#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
CHAT_URL="${BASE_URL%/}/api/v1/chat"
SESSION_ID="experiment-e-$(date +%s)-$$"
COMPLETED=0

command -v curl >/dev/null || { echo "ERROR curl is required" >&2; exit 1; }
command -v jq >/dev/null || { echo "ERROR jq is required" >&2; exit 1; }

PROMPTS=(
  "Memoriza el marcador sintetico temprano ALFA-006 y responde confirmado."
  "Turno de relleno 02: responde con el numero 02."
  "Turno de relleno 03: responde con el numero 03."
  "Turno de relleno 04: responde con el numero 04."
  "Turno de relleno 05: responde con el numero 05."
  "Turno de relleno 06: responde con el numero 06."
  "Turno de relleno 07: responde con el numero 07."
  "Turno de relleno 08: responde con el numero 08."
  "Turno de relleno 09: responde con el numero 09."
  "Punto temprano: indica si el marcador ALFA-006 sigue en el contexto."
  "Turno de ventana 11: responde con el numero 11."
  "Turno de ventana 12: responde con el numero 12."
  "Turno de ventana 13: responde con el numero 13."
  "Turno de ventana 14: responde con el numero 14."
  "Turno de ventana 15: responde con el numero 15."
  "Turno de ventana 16: responde con el numero 16."
  "Turno de ventana 17: responde con el numero 17."
  "Turno de ventana 18: responde con el numero 18."
  "Memoriza el marcador sintetico reciente OMEGA-006 y responde confirmado."
  "Punto final: indica si recuerdas OMEGA-006 y si ALFA-006 ya no aparece en el contexto disponible."
)

if [ "${#PROMPTS[@]}" -ne 20 ]; then
  echo "ERROR experiment must contain exactly 20 prompts" >&2
  exit 1
fi

fail_turn() {
  local turn="$1" reason="$2"
  echo "FAIL turn=$turn reason=$reason" >&2
  echo "SUMMARY session=$SESSION_ID completed=$COMPLETED failures=1 recent_context=not-completed oldest_context=not-completed" >&2
  exit 1
}

for index in "${!PROMPTS[@]}"; do
  turn=$((index + 1))
  payload=$(jq -cn \
    --arg question "${PROMPTS[$index]}" \
    --arg sessionId "$SESSION_ID" \
    '{question:$question,sessionId:$sessionId,templateId:"conciso",temperature:0,seed:7,numPredict:160}')

  if ! result=$(curl --max-time 240 -sS -w $'\n%{http_code}' \
      -H 'Content-Type: application/json' -d "$payload" "$CHAT_URL"); then
    fail_turn "$turn" "transport"
  fi
  code="${result##*$'\n'}"
  body="${result%$'\n'*}"
  [ "$code" = "200" ] || fail_turn "$turn" "http-$code"

  if ! echo "$body" | jq -e '
      (.requestId | type == "string" and test("\\S")) and
      (.answer | type == "string" and test("\\S")) and
      (.elapsedMs | type == "number" and . >= 0) and
      .status == "ok" and
      has("promptTokens") and (.promptTokens == null or (.promptTokens | type == "number")) and
      has("completionTokens") and (.completionTokens == null or (.completionTokens | type == "number")) and
      has("totalTokens") and (.totalTokens == null or (.totalTokens | type == "number")) and
      has("model") and (.model == null or (.model | type == "string")) and
      has("finishReason") and (.finishReason == null or (.finishReason | type == "string")) and
      has("tokensPerSecond") and (.tokensPerSecond == null or (.tokensPerSecond | type == "number")) and
      has("errors") and .errors == null
    ' >/dev/null; then
    fail_turn "$turn" "contract"
  fi

  COMPLETED=$turn
  preview=$(echo "$body" | jq -r '.answer | gsub("[\\r\\n\\t]+"; " ") | .[0:160]')
  label="ordinary"
  [ "$turn" -eq 10 ] && label="early-context-checkpoint"
  [ "$turn" -eq 20 ] && label="recent-and-eviction-checkpoint"
  printf 'PASS turn=%02d label=%s answer_preview=%s\n' "$turn" "$label" "$preview"
done

echo "SUMMARY session=$SESSION_ID completed=$COMPLETED failures=0 recent_context=inspect-turn-20 oldest_context=inspect-turn-20-for-eviction"
