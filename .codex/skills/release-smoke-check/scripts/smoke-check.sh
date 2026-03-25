#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

FRONTEND_URL="${FRONTEND_URL:-http://localhost:3000}"
BACKEND_HEALTH_URL="${BACKEND_HEALTH_URL:-http://localhost:8080/api/public/health}"
REGISTRATION_POLICY_URL="${REGISTRATION_POLICY_URL:-http://localhost:3000/api/registration/policy}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
MAILPIT_URL="${MAILPIT_URL:-http://localhost:8025}"

check_url() {
  local label="$1"
  local url="$2"

  local status
  status="$(curl -sS -o /tmp/heuermannplus-smoke.out -w "%{http_code}" "$url")"
  if [[ "$status" != "200" ]]; then
    echo "[FAIL] $label -> $url returned HTTP $status"
    cat /tmp/heuermannplus-smoke.out
    return 1
  fi

  echo "[PASS] $label -> $url"
}

echo "Repo root: $ROOT_DIR"
check_url "Frontend" "$FRONTEND_URL"
check_url "Backend health" "$BACKEND_HEALTH_URL"
check_url "Registration policy" "$REGISTRATION_POLICY_URL"
check_url "Keycloak" "$KEYCLOAK_URL"
check_url "Mailpit" "$MAILPIT_URL"

echo "Quick smoke checks passed."
