---
name: release-smoke-check
description: Run a repo-specific local smoke check for HeuermannPlus after frontend, backend, auth, or runtime changes. Use when validating the local stack, demo readiness, or high-risk flows like registration, verification, and login.
---

# Release Smoke Check

## Overview

Use this skill after meaningful changes to confirm the local stack still behaves correctly. Start with non-destructive checks, then run the browser-based registration and login flow when the change affects user-facing or auth-sensitive behavior.

## Workflow

1. Bring up the stack with `$heuermannplus-compose-rebuild` when needed.
2. Run `scripts/smoke-check.sh` for quick service availability and core endpoint checks.
3. If registration, login, or auth behavior changed, perform the browser flow:
   - open `/register`
   - confirm registration policy loads
   - register a new user with captcha token `test-pass`
   - get the verification link from Mailpit
   - verify the account via `/register/verify`
   - log in from the homepage with Keycloak
4. Report both what passed and what was not exercised.

## Repo Rules

- Use unique test emails and nicknames for each smoke run.
- Avoid destructive Compose resets unless the Keycloak client drift issue clearly matches the observed failure.
- Treat Mailpit and Keycloak as first-class checkpoints, not optional extras.

## Resources

- `scripts/smoke-check.sh`: Quick non-destructive health and reachability checks.
- `references/smoke-checklist.md`: Repo-specific happy path and validation checkpoints.
