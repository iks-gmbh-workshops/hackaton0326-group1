---
name: heuermannplus-compose-rebuild
description: Recreate the HeuermannPlus Docker Compose stack for this repo with the safe default `docker compose up -d --build`. Use when asked to recreate, rebuild, restart, or bring up the local HeuermannPlus containers, or when Codex needs the repo-specific Compose workflow and recovery notes.
---

# HeuermannPlus Compose Rebuild

## Overview

Use this skill to bring the local HeuermannPlus Docker stack back up with a safe rebuild. Default to `docker compose up -d --build` and keep destructive reset commands out of the normal path.

## Standard Workflow

1. Use `scripts/recreate-compose.sh` for the actual rebuild command.
2. Use `--dry-run` first when validating the setup or when the user wants to preview the command.
3. Run the script without `--dry-run` for the normal rebuild path.
4. Report the main local endpoints after the command completes.

## Escalation Rules

Use `docker compose down -v` only when one of these is true:

- the user explicitly asks for a destructive reset
- the known Keycloak registration issue appears and the normal rebuild does not resolve it

Warn clearly that `down -v` removes Compose volumes and resets persisted local data before suggesting it. After a destructive reset, rerun the normal rebuild path.

## Resources

- `scripts/recreate-compose.sh`: Resolve the repo root, validate Docker Compose availability, optionally dry-run, and execute `docker compose up -d --build`.
- `references/compose-notes.md`: Read the HeuermannPlus stack overview, default URLs, and the repo-specific recovery note before suggesting escalations.
