# HeuermannPlus

Multilayer-Web-App-Scaffold mit Next.js im Frontend, Spring Boot mit Kotlin im Backend, Keycloak fuer Authentifizierung und zwei getrennten PostgreSQL-Instanzen fuer App- und IAM-Daten.

## Stack

- Frontend: Next.js 15.5.9, React 19.1.1, TypeScript 5.9.2, Tailwind CSS 4.1.13, DaisyUI 5.1.7
- Backend: Spring Boot 4.0.3, Kotlin 2.2.20, Gradle 9.4.0, Java 25 Runtime mit Bytecode-Target 24
- Auth: Keycloak 26.5.5
- Persistenz: PostgreSQL 18.3 fuer App und Keycloak
- Lokales Setup: Docker Compose im Repo-Root

## Projektstruktur

```text
.
|-- backend
|-- docs
|-- frontend
|-- keycloak
|-- .editorconfig
|-- .env
|-- .gitignore
|-- docker-compose.yml
`-- README.md
```

## Schnellstart

1. Repo clonen und ins Projektverzeichnis wechseln.
2. Optional Werte in `.env` anpassen.
3. Stack starten:

```bash
docker compose up --build
```

4. Anwendungen aufrufen:

- Frontend: `http://localhost:3000`
- Backend Health: `http://localhost:8080/api/public/health`
- Keycloak: `http://localhost:8081`
- Mailpit UI: `http://localhost:8025`

## Lokale Zugange

- Keycloak Admin: `admin` / `Admin123!`
- Anwendungsnutzer: lokal ueber die Registrierungsstrecke im Frontend anlegen

## Auth-Demo

- Das Frontend startet den Login gegen Keycloak.
- Nach erfolgreichem Login speichert `next-auth` die Session JWT-basiert.
- Geschuetzte Frontend-Calls gehen direkt aus dem Browser an das Backend und senden den Access Token als Bearer-JWT.
- Das Backend validiert den Bearer-Token gegen Keycloak als OAuth2 Resource Server.

## Registrierung lokal

- Die Compose-Umgebung enthaelt jetzt Mailpit fuer Verifizierungs-E-Mails und einen technischen Keycloak-Client fuer die Registrierungslogik.
- Im Default-Setup laeuft Captcha lokal im Mock-Modus; verwende dafuer den Token `test-pass`.
- Wenn ein bestehendes Keycloak-Postgres-Volume schon vor der Realm-Erweiterung angelegt wurde, zieht `--import-realm` die neuen Clients und Rollen nicht nach.
- Typisches Symptom davon bei der Registrierung: Im Backend erscheint `401 invalid_client`, waehrend Keycloak selbst `client_not_found` fuer `heuermannplus-registration-service` loggt.
- In diesem Fall den Stack fuer einen kompletten lokalen Neuimport einmal mit geloeschten Volumes starten:

```bash
docker compose down -v
docker compose up --build
```

## Entwicklung ohne Docker

- Frontend: Node.js 24.x LTS und `npm install && npm run dev` in `frontend/`
- Backend: Java 25 und `./gradlew bootRun` in `backend/`

Die Root-Compose-Datei bleibt trotzdem der bevorzugte lokale Einstieg, weil alle Abhaengigkeiten damit konsistent hochfahren.

## Dev Container

Ein VS-Code-Dev-Container liegt unter `.devcontainer/` und ist auf diesen Repo-Stack zugeschnitten.

- Vorinstalliert: Node.js 24.x, npm, Java 25, Docker-CLI-Zugriff auf den Host-Daemon und `@openai/codex`
- Beim ersten Container-Start werden `frontend`-Dependencies installiert und der Gradle Wrapper in `backend` vorgewaermt
- Der Root-Stack aus `docker-compose.yml` startet nicht automatisch und bleibt bewusst manuell

Nutzung:

1. In VS Code `Dev Containers: Reopen in Container` ausfuehren.
2. Nach dem ersten Bootstrap bei Bedarf den App-Stack manuell starten:

```bash
docker compose up -d
```

3. Codex im Container manuell authentifizieren:

```bash
codex login
```

Alternativ kann ein API-Key wie `OPENAI_API_KEY` manuell im Container gesetzt werden. Die Dev-Container-Konfiguration reicht keine OpenAI-Credentials automatisch durch.
