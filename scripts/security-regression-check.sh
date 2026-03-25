#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "$ROOT_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.env"
  set +a
fi

BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:${BACKEND_PORT:-8080}}"

unauthorized_status="$(curl -sS -o /tmp/heuermannplus-unauthorized.$$ -w "%{http_code}" "$BACKEND_URL/api/private/me")"
rm -f /tmp/heuermannplus-unauthorized.$$
if [[ "$unauthorized_status" != "401" ]]; then
  echo "Unautorisierter Zugriff erwartet 401, bekam $unauthorized_status" >&2
  exit 1
fi

terms_status="$(curl -sS -o /tmp/heuermannplus-terms.$$ -w "%{http_code}" \
  -H 'Content-Type: application/json' \
  -d '{"nickname":"qa-security","password":"Drum123!","passwordRepeat":"Drum123!","email":"qa-security@heuermannplus.local","captchaToken":"test-pass","acceptTerms":false}' \
  "$BACKEND_URL/api/public/registration")"
terms_body="$(cat /tmp/heuermannplus-terms.$$)"
rm -f /tmp/heuermannplus-terms.$$

if [[ "$terms_status" != "400" ]]; then
  echo "Registrierung ohne AGB erwartet 400, bekam $terms_status" >&2
  exit 1
fi

if [[ "$terms_body" != *'"code":"TERMS_NOT_ACCEPTED"'* ]]; then
  echo "Registrierung ohne AGB lieferte nicht TERMS_NOT_ACCEPTED" >&2
  exit 1
fi

echo "Security-Regression-Checks erfolgreich."
