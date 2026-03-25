# Smoke Check Checklist

## Core Local URLs

- Frontend: `http://localhost:3000`
- Backend health: `http://localhost:8080/api/public/health`
- Keycloak: `http://localhost:8081`
- Mailpit UI: `http://localhost:8025`

## Quick Checks

- Frontend home page returns HTTP 200
- Backend health returns HTTP 200
- Registration policy returns HTTP 200
- Keycloak realm page is reachable
- Mailpit UI is reachable

## Stateful Flow

Use when registration or auth changed:

1. Open `/register`
2. Fill the form with a fresh nickname and email
3. Use captcha token `test-pass` in local mock mode
4. Accept the current terms
5. Submit and confirm success feedback
6. Open Mailpit and extract the verification link
7. Visit `/register/verify?token=...`
8. Return to `/` and use `Mit Keycloak anmelden`
9. Confirm the protected authenticated flow still works
