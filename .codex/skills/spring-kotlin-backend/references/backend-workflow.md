# Backend Workflow Notes

- Main backend code lives in `backend/src/main/kotlin/de/heuermannplus/backend/`.
- Tests live in `backend/src/test/kotlin/de/heuermannplus/backend/`.
- Flyway migrations live in `backend/src/main/resources/db/migration/`.
- Runtime config lives in `backend/src/main/resources/application.yml`.

## Common Commands

From `backend/`:

```bash
./gradlew test
./gradlew bootRun
```

## Repo-Specific Guidance

- The backend uses Spring Boot 4, Kotlin, JPA, and Flyway.
- Registration and auth-adjacent logic already exists and touches Keycloak-facing integration code.
- The project uses PostgreSQL in normal runtime, but some tests use H2 in PostgreSQL compatibility mode.
- When adding migrations, avoid assuming PostgreSQL-only SQL will pass in H2-backed tests unless the affected test path is isolated accordingly.
- Read relevant product docs in `docs/` before changing intended behavior.
