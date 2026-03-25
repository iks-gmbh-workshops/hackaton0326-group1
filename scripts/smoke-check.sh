#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

FRONTEND_URL="${FRONTEND_URL:-http://127.0.0.1:${FRONTEND_PORT:-3000}}"
BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:${BACKEND_PORT:-8080}}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://127.0.0.1:${KEYCLOAK_PORT:-8081}}"
MAILPIT_URL="${MAILPIT_URL:-http://127.0.0.1:${MAILPIT_UI_PORT:-8025}}"
REALM="${KEYCLOAK_REALM:-heuermannplus}"

wait_for_url() {
  local url="$1"
  local label="$2"

  for _ in $(seq 1 60); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "[ok] $label"
      return 0
    fi
    sleep 2
  done

  echo "[fail] $label: $url nicht erreichbar" >&2
  return 1
}

assert_contains() {
  local body="$1"
  local needle="$2"
  local label="$3"

  if [[ "$body" != *"$needle"* ]]; then
    echo "[fail] $label: erwartete Zeichenfolge '$needle' nicht gefunden" >&2
    exit 1
  fi

  echo "[ok] $label"
}

wait_for_url "$FRONTEND_URL" "Frontend erreichbar"
wait_for_url "$BACKEND_URL/api/public/health" "Backend Public Health erreichbar"
wait_for_url "$KEYCLOAK_URL/realms/$REALM/.well-known/openid-configuration" "Keycloak Realm erreichbar"
wait_for_url "$MAILPIT_URL/api/v1/messages" "Mailpit API erreichbar"

public_health="$(curl -fsS "$BACKEND_URL/api/public/health")"
assert_contains "$public_health" "\"status\":\"UP\"" "Public Health meldet UP"

actuator_health="$(curl -fsS "$BACKEND_URL/actuator/health")"
assert_contains "$actuator_health" "\"status\":\"UP\"" "Actuator Health meldet UP"

policy="$(curl -fsS "$BACKEND_URL/api/public/registration/policy")"
assert_contains "$policy" "\"currentVersion\"" "Registration Policy liefert AGB-Version"
assert_contains "$policy" "\"mode\":\"mock\"" "Registration Policy ist lokal im Captcha-Mock-Modus"

private_status="$(curl -sS -o /tmp/heuermannplus-private-body.$$ -w "%{http_code}" "$BACKEND_URL/api/private/me")"
rm -f /tmp/heuermannplus-private-body.$$
if [[ "$private_status" != "401" ]]; then
  echo "[fail] Private Endpoint sollte ohne Token 401 liefern, war aber $private_status" >&2
  exit 1
fi
echo "[ok] Private Endpoint verweigert anonyme Zugriffe"

echo "Smoke-Check erfolgreich."
