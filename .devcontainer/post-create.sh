#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "[devcontainer] Installing OpenAI Codex CLI globally..."
if ! command -v npm >/dev/null 2>&1; then
  echo "[devcontainer] npm was not found on PATH. Ensure the Node devcontainer feature finished installing correctly." >&2
  exit 1
fi
npm install -g @openai/codex

echo "[devcontainer] Installing frontend dependencies..."
cd "${REPO_ROOT}/frontend"
npm install

echo "[devcontainer] Warming Gradle wrapper and validating Java toolchain..."
cd "${REPO_ROOT}/backend"
if [[ ! -f ./gradlew ]]; then
  echo "[devcontainer] Gradle wrapper is missing from backend/." >&2
  exit 1
fi

# Gradle may leave project caches in the bind-mounted workspace with root ownership
# (for example after Docker-based builds). Repair them when possible and keep the
# devcontainer bootstrap on a user-owned cache path.
for gradle_path in "${REPO_ROOT}/backend/.gradle" "${REPO_ROOT}/backend/.kotlin" "${REPO_ROOT}/backend/build"; do
  if [[ -e "${gradle_path}" ]]; then
    sudo chown -R "$(id -u):$(id -g)" "${gradle_path}" 2>/dev/null || true
    chmod -R u+rwX "${gradle_path}" 2>/dev/null || true
  fi
done

GRADLE_PROJECT_CACHE_DIR="${HOME}/.cache/gradle-project-cache/backend"
mkdir -p "${GRADLE_PROJECT_CACHE_DIR}"
./gradlew --no-daemon --project-cache-dir "${GRADLE_PROJECT_CACHE_DIR}" help

echo "[devcontainer] Bootstrap complete."
echo "[devcontainer] Docker Compose remains stopped by default."
echo "[devcontainer] Authenticate Codex manually with 'codex login' or by setting OPENAI_API_KEY."
