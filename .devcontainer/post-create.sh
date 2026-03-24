#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "[devcontainer] Installing OpenAI Codex CLI globally..."
sudo npm install -g @openai/codex

echo "[devcontainer] Installing frontend dependencies..."
cd "${REPO_ROOT}/frontend"
npm install

echo "[devcontainer] Warming Gradle wrapper and validating Java toolchain..."
cd "${REPO_ROOT}/backend"
./gradlew --no-daemon help

echo "[devcontainer] Bootstrap complete."
echo "[devcontainer] Docker Compose remains stopped by default."
echo "[devcontainer] Authenticate Codex manually with 'codex login' or by setting OPENAI_API_KEY."
