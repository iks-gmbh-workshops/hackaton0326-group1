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

measure_ms() {
  local url="$1"
  python3 - <<'PY' "$url"
import sys
import time
import urllib.request

url = sys.argv[1]
start = time.perf_counter()
with urllib.request.urlopen(url, timeout=10) as response:
    response.read()
elapsed_ms = int((time.perf_counter() - start) * 1000)
print(elapsed_ms)
PY
}

frontend_ms="$(measure_ms "$FRONTEND_URL")"
backend_ms="$(measure_ms "$BACKEND_URL/api/public/health")"

echo "Frontend response time: ${frontend_ms}ms"
echo "Backend health response time: ${backend_ms}ms"

if (( frontend_ms > 4000 )); then
  echo "Frontend response time exceeds 4000ms budget" >&2
  exit 1
fi

if (( backend_ms > 2000 )); then
  echo "Backend health response time exceeds 2000ms budget" >&2
  exit 1
fi

echo "Runtime-Budget-Check erfolgreich."
