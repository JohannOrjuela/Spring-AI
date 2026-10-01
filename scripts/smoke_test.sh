#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
command -v jq >/dev/null || { echo "jq is required"; exit 1; }

check() {
  local endpoint="$1" payload="$2" expected="$3" result code body
  result=$(curl --max-time 240 -sS -w $'\n%{http_code}' -H 'Content-Type: application/json' -d "$payload" "$BASE_URL/api/v1/$endpoint")
  code="${result##*$'\n'}"
  body="${result%$'\n'*}"
  [ "$code" = "$expected" ] || { echo "FAIL $endpoint: expected $expected, received $code"; exit 1; }
  echo "$body" | jq -e '
    (.requestId | type == "string" and length > 0) and
    ([ "elapsedMs", "status", "promptTokens", "completionTokens", "totalTokens", "model", "finishReason", "tokensPerSecond", "errors" ] - keys | length == 0)
  ' >/dev/null
  if [ "$expected" = 400 ]; then
    echo "$body" | jq -e '.status == "error" and .answer == "Request validation failed" and (.errors | length > 0) and .elapsedMs == 0 and .promptTokens == null and .completionTokens == null and .totalTokens == null and .model == null and .finishReason == null and .tokensPerSecond == null' >/dev/null
  elif [ "$endpoint" = classifications ]; then
    echo "$body" | jq -e '.status == "ok" and .answer == null and (.classification.categoria | type == "string" and test("\\S")) and (.classification.justificacion | type == "string" and test("\\S")) and (.classification.confianza | type == "number" and . >= 0 and . <= 100 and floor == .)' >/dev/null
  else
    echo "$body" | jq -e '.status == "ok" and (.answer | type == "string" and length > 0)' >/dev/null
  fi
  echo "PASS $endpoint HTTP $expected"
}
check chat '{"question":"Responde solamente listo."}' 200
check chat '{"question":"Explica brevemente una interfaz Java.","templateId":"tutor","rol":"docente","dominio":"programacion","idioma":"espanol","temperature":0,"topK":0,"topP":0.9,"numPredict":100,"seed":7}' 200
SMOKE_SESSION="smoke-memory-$$"
check chat "$(jq -cn --arg id "$SMOKE_SESSION" '{question:"Conserva este dato sintetico para el siguiente turno.",sessionId:$id,templateId:"conciso",temperature:0,seed:7}')" 200
check chat "$(jq -cn --arg id "$SMOKE_SESSION" '{question:"Continua usando el contexto de esta misma sesion.",sessionId:$id,templateId:"tutor",rol:"verificador",dominio:"memoria",idioma:"espanol",temperature:0,seed:7}')" 200
check chat "$(jq -cn --arg id "Case" '{question:"Prueba de identificador exacto.",sessionId:$id}')" 200
check chat "$(jq -cn --arg id "case" '{question:"Prueba de identificador exacto.",sessionId:$id}')" 200
check chat "$(jq -cn --arg id " Case " '{question:"Prueba de identificador exacto.",sessionId:$id}')" 200
check chat '{"question":"x","sessionId":" "}' 400
check chat '{"question":"x","templateId":"../../secret"}' 400
check chat '{"question":"x","systemPrompt":"client instructions"}' 400
check chat '{"question":"x","temperature":17,"numPredict":4096}' 400
check classifications '{"text":"No puedo iniciar sesion.","rol":"analista","dominio":"soporte","idioma":"espanol","temperature":0,"numPredict":256}' 200
check classifications '{"text":"x","templateId":"tutor"}' 400
echo "Feature 006 bounded smoke contracts passed"
