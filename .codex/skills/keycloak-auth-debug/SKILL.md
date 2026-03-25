---
name: keycloak-auth-debug
description: Debug or change Keycloak, login, token, registration, and role-mapping flows in this repo. Use when authentication fails, registration breaks, tokens are rejected, realm clients or roles drift, or frontend, backend, and Keycloak configuration must be traced together.
---

# Keycloak Auth Debug

## Overview

Use this skill for trust-boundary and auth-flow work that crosses `frontend/`, `backend/`, and `keycloak/`. Follow the repo's configured clients, roles, and local recovery notes before proposing deeper changes.

## Workflow

1. Read `README.md`, `keycloak/realm/heuermannplus-realm.json`, `frontend/lib/auth.ts`, and `backend/src/main/resources/application.yml`.
2. Trace the affected flow end to end:
   - browser and NextAuth config in `frontend/`
   - backend token validation or registration logic in `backend/`
   - realm, clients, and roles in `keycloak/`
3. Confirm whether the issue is code, config drift, or stale local state.
4. Prefer the safe Compose rebuild path first for local environment drift.
5. Suggest destructive resets only when the known Keycloak volume issue matches the observed symptoms.

## Repo Rules

- Do not change token, role, or permission behavior casually.
- Keep public and internal Keycloak URLs straight; the repo uses both.
- When auth-sensitive behavior changes, call out likely Security review needs.

## Resources

- `references/keycloak-notes.md`: Repo-specific clients, roles, URLs, and the known registration recovery path.
