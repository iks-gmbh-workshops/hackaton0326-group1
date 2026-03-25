---
name: flyway-postgres-migrations
description: Add or review database migrations in this repo's backend. Use when changing schema, seed data, persistence models, or migration compatibility across PostgreSQL runtime and H2-backed tests.
---

# Flyway PostgreSQL Migrations

## Overview

Use this skill when backend changes require schema evolution. Keep migrations aligned with the repo's Flyway layout, JPA entities, and test setup instead of treating SQL changes in isolation.

## Workflow

1. Read the affected entity, store/repository code, and the existing migrations in `backend/src/main/resources/db/migration/`.
2. Add a new sequential Flyway migration; do not rewrite old migrations.
3. Update entities and persistence code together with the migration.
4. Check whether the SQL will run in both PostgreSQL and the repo's H2-based tests.
5. Validate with targeted backend tests and note any compatibility caveats.

## Repo Rules

- Prefer straightforward SQL over database-specific tricks unless clearly needed.
- Be careful with partial indexes, advanced DDL, and dialect-specific expressions; this repo's tests may exercise migrations in H2.
- Seed only the minimum data required for the feature or local bootstrapping.

## Resources

- `references/migration-notes.md`: Repo-specific migration locations, compatibility notes, and validation steps.
