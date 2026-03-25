# HeuermannPlus Compose Notes

## Service Overview

- `app-postgres`: PostgreSQL for application data
- `keycloak-postgres`: PostgreSQL for Keycloak data
- `keycloak`: local Keycloak instance built from `./keycloak`
- `mailpit`: SMTP sink and inbox UI for registration emails
- `backend`: Spring Boot backend built from `./backend`
- `frontend`: Next.js frontend built from `./frontend`

## Default Startup Command

```bash
docker compose up -d --build
```

## Default Local URLs

- Frontend: `http://localhost:3000`
- Backend health: `http://localhost:8080/api/public/health`
- Keycloak: `http://localhost:8081`
- Mailpit UI: `http://localhost:8025`

## Recovery Note

If registration starts failing after realm or client changes, a normal rebuild may not be enough when old Keycloak Postgres volumes still exist.

Typical symptom:

- backend shows `401 invalid_client`
- Keycloak logs show `client_not_found` for `heuermannplus-registration-service`

Escalation path:

```bash
docker compose down -v
docker compose up -d --build
```

`docker compose down -v` is destructive. It deletes Compose volumes and resets persisted local data, so do not use it as the default recreate path.
