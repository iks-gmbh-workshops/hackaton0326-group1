# Teststrategie fuer HeuermannPlus

## Zielbild

Diese Testbasis deckt HeuermannPlus in mehreren Stufen ab, damit Pull Requests schnelles Feedback bekommen und die risikoreichen Registrierungs-, Verifizierungs- und Auth-Fluesse trotzdem gegen die echte Compose-Umgebung abgesichert bleiben.

## Testpyramide

- Static / Fast Feedback
  - Frontend: `npm run lint`, `npm run typecheck`, `npm run test:unit`
  - Backend: `./gradlew test --tests '*Controller*' --tests '*RegistrationServiceTest'`
- Unit- und Component-Tests
  - Frontend: Vitest, React Testing Library, `@testing-library/jest-dom`, `user-event`, MSW
  - Backend: fokussierte Service-Tests wie `RegistrationServiceTest`
- Slice-Tests
  - Backend Web: `@WebMvcTest` fuer oeffentliche und geschuetzte Endpunkte
  - Backend JPA: `@DataJpaTest` fuer Persistenz und Constraints
- Integrations-Tests
  - Backend PostgreSQL: Testcontainers-basierte Wahrheitstests fuer Flyway und Persistenz
- Smoke-Checks
  - `scripts/smoke-check.sh` gegen den laufenden Compose-Stack
- End-to-End
  - Playwright gegen die echte Compose-Umgebung inklusive Mailpit und Keycloak
- Nicht-funktional
  - `scripts/security-regression-check.sh`
  - `scripts/runtime-budget-check.sh`

## Kritische Fluesse und ihre Testebenen

- Homepage und Runtime-Verfuegbarkeit
  - Smoke, Playwright
- Registration Policy, Formularvalidierung, Fehleranzeige und Nickname-Vorschlaege
  - Frontend Unit/Component, Backend WebMvc, Smoke
- Registrierung mit AGB und Captcha-Mock
  - Backend Service, Frontend Component, Playwright, Security Regression
- Verifizierung via Mailpit-Link
  - Backend Service, Playwright
- Login ueber Keycloak und BFF `/api/me`
  - Frontend Route-Test, Backend WebMvc, Playwright
- Persistenz und Migrationspfade
  - H2-Slice-Tests und PostgreSQL-Testcontainers

## Lokale Kommandos

Frontend:

```bash
cd frontend
npm ci
npm run lint
npm run typecheck
npm run test:unit
npm run test:e2e
```

Backend:

```bash
cd backend
./gradlew test
./gradlew test --tests '*Postgres*'
```

Stack und Smoke:

```bash
docker compose up -d --build
./scripts/smoke-check.sh
./scripts/security-regression-check.sh
./scripts/runtime-budget-check.sh
```

## CI-Stufen

Die GitHub-Action unter [`.github/workflows/qa.yml`](/workspaces/hackaton0326-group1/.github/workflows/qa.yml) fuehrt folgende Stufen aus:

1. `frontend-quality`: Lint, Typecheck und Frontend-Unit-/Component-Tests
2. `backend-quality`: schnelle Backend-Unit- und Web-Slice-Tests
3. `backend-postgres-integration`: PostgreSQL-/Flyway-nahe Integrationstests
4. `compose-smoke`: Compose-Start und Smoke-Checks gegen Frontend, Backend, Keycloak und Mailpit
5. `e2e-playwright`: Browser-Flows fuer Registration, Verifizierung, Login und BFF
6. `nightly-regressions`: nur nachts, fuer Security- und Runtime-Budgets

## Testdaten und Hinweise

- Lokaler Captcha-Mock verwendet standardmaessig den Token `test-pass`.
- Playwright erzeugt pro Lauf eindeutige E-Mail-Adressen und Nicknames.
- Der Login im E2E-Flow nutzt den Demo-User aus [README.md](/workspaces/hackaton0326-group1/README.md).
- Wenn Keycloak-Clients in einer alten lokalen Volume-Version fehlen, zuerst den in der README beschriebenen Realm-Neuimport mit geloeschten Volumes ausfuehren.

## Reviewer-Fokus

- Backend Developer: Slice- und PostgreSQL-Testabdeckung fuer Controller, Persistenz und Migrationen
- Frontend Developer: Vitest-/RTL-/MSW-Basis und BFF-/UI-Tests
- Security: anonyme Zugriffe, AGB-Zwang und Auth-/Profil-Fehlerpfade
- Ops: Compose-, Mailpit-, Keycloak- und lokale CI-Lauffaehigkeit
