# Migration Notes

## Locations

- Flyway migrations: `backend/src/main/resources/db/migration/`
- Backend entities and stores: `backend/src/main/kotlin/de/heuermannplus/backend/`
- Backend tests: `backend/src/test/kotlin/de/heuermannplus/backend/`

## Validation

From `backend/`:

```bash
./gradlew test
```

## Repo-Specific Compatibility Reminder

- Runtime uses PostgreSQL.
- Some JPA and persistence tests use H2 in PostgreSQL compatibility mode.
- If a migration uses SQL that H2 does not understand, tests may fail during Flyway startup before business logic is exercised.
- When a PostgreSQL-specific feature is necessary, call out the compatibility impact explicitly and keep the test strategy in mind.
