# Keycloak Notes

## Core Files

- Realm config: `keycloak/realm/heuermannplus-realm.json`
- Frontend auth config: `frontend/lib/auth.ts`
- Backend app config: `backend/src/main/resources/application.yml`
- Local runtime and recovery notes: `README.md`

## Clients And Roles

- Frontend client: `heuermannplus-frontend`
- Registration service client: `heuermannplus-registration-service`
- Roles seen in the repo: `registration-pending`, `app-user`

## URL Split

- Public browser-facing Keycloak base defaults to `http://localhost:8081/...`
- Internal service-to-service Keycloak base defaults to `http://keycloak:8080/...`

## Known Local Failure Mode

If registration starts failing after realm/client changes:

- backend symptom: `401 invalid_client`
- Keycloak symptom: `client_not_found` for `heuermannplus-registration-service`

Preferred recovery order:

1. Safe rebuild via the Compose rebuild skill
2. If still broken and symptoms match, warn about data loss and use:

```bash
docker compose down -v
docker compose up -d --build
```
