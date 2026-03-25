#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: recreate-compose.sh [--repo-root PATH] [--dry-run]

Safely recreate the HeuermannPlus Docker Compose stack with:
  docker compose up -d --build
EOF
}

repo_root=""
dry_run=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo-root)
      if [[ $# -lt 2 ]]; then
        echo "Error: --repo-root requires a path." >&2
        usage >&2
        exit 1
      fi
      repo_root="$2"
      shift 2
      ;;
    --dry-run)
      dry_run=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Error: unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
default_repo_root="$(cd -- "$script_dir/../../../.." && pwd)"
repo_root="${repo_root:-$default_repo_root}"
repo_root="$(cd -- "$repo_root" && pwd)"

if ! command -v docker >/dev/null 2>&1; then
  echo "Error: docker is not installed or not on PATH." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Error: 'docker compose' is not available." >&2
  exit 1
fi

if [[ ! -f "$repo_root/docker-compose.yml" ]]; then
  echo "Error: docker-compose.yml not found at $repo_root" >&2
  exit 1
fi

if [[ ! -f "$repo_root/.env" ]]; then
  echo "Warning: .env not found at $repo_root/.env; Compose will rely on shell environment values and defaults." >&2
fi

cmd=(docker compose up -d --build)

echo "Repo root: $repo_root"
echo "Command: ${cmd[*]}"

if [[ "$dry_run" -eq 1 ]]; then
  echo "Dry run only; command not executed."
  exit 0
fi

(
  cd "$repo_root"
  "${cmd[@]}"
)

cat <<'EOF'
Rebuild complete.

Default local endpoints:
- Frontend: http://localhost:3000
- Backend Health: http://localhost:8080/api/public/health
- Keycloak: http://localhost:8081
- Mailpit UI: http://localhost:8025

These URLs assume the default .env values from this repo.
EOF
