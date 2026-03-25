---
name: spring-kotlin-backend
description: Implement or review backend work in this repo's Spring Boot and Kotlin application. Use when touching controllers, services, validation, persistence, configuration, or tests under `backend/`, especially when Flyway, JPA, or registration and auth-adjacent backend code are involved.
---

# Spring Kotlin Backend

## Overview

Use this skill for backend tasks in `backend/`. Align work with the repo's existing Spring Boot 4, Kotlin, JPA, Flyway, and test conventions instead of inventing new patterns.

## Workflow

1. Read `README.md`, relevant files in `docs/`, and `backend/src/main/resources/application.yml`.
2. Inspect the touched area in `backend/src/main` and the matching tests in `backend/src/test`.
3. Keep behavior in services and persistence layers consistent with existing code before introducing new abstractions.
4. When schema changes are needed, add a Flyway migration and check compatibility with both PostgreSQL and the repo's H2-based tests.
5. Validate with targeted `./gradlew test` runs from `backend/`.

## Repo Rules

- Prefer extending existing service and store patterns over creating parallel ones.
- Keep validation errors explicit and field-oriented when the API already follows that pattern.
- Treat `docs/` as authoritative project guidance when relevant docs exist.
- Do not change auth or trust-boundary behavior silently; flag it for Security-aware review.

## Resources

- `references/backend-workflow.md`: Repo-specific backend structure, commands, and migration/testing notes.
