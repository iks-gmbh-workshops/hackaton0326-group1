# HeuermannPlus

Multilayer-Web-App-Scaffold mit Next.js im Frontend, Spring Boot mit Kotlin im Backend, Keycloak für Authentifizierung und zwei getrennten PostgreSQL-Instanzen für App- und IAM-Daten.

## Stack

- Frontend: Next.js 15.5.9, React 19.1.1, TypeScript 5.9.2, Tailwind CSS 4.1.13, DaisyUI 5.1.7
- Backend: Spring Boot 4.0.3, Kotlin 2.2.20, Gradle 9.4.0, Java 25 Runtime mit Bytecode-Target 24
- Auth: Keycloak 26.5.5
- Persistenz: PostgreSQL 18.3 für App und Keycloak
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

## Lokale Zugänge

- Keycloak Admin: `admin` / `Admin123!`
- Anwendungsnutzer: lokal über die Registrierungsstrecke im Frontend anlegen

## Auth-Demo

- Das Frontend startet den Login gegen Keycloak.
- Nach erfolgreichem Login speichert `next-auth` die Session JWT-basiert.
- Geschützte Frontend-Calls gehen direkt aus dem Browser an das Backend und senden den Access Token als Bearer-JWT.
- Das Backend validiert den Bearer-Token gegen Keycloak als OAuth2 Resource Server.

## Registrierung lokal

- Die Compose-Umgebung enthält jetzt Mailpit für Verifizierungs-E-Mails und einen technischen Keycloak-Client für die Registrierungslogik.
- Im Default-Setup läuft Captcha lokal im Mock-Modus; verwende dafür den Token `test-pass`.
- Wenn ein bestehendes Keycloak-Postgres-Volume schon vor der Realm-Erweiterung angelegt wurde, zieht `--import-realm` die neuen Clients und Rollen nicht nach.
- Typisches Symptom davon bei der Registrierung: Im Backend erscheint `401 invalid_client`, während Keycloak selbst `client_not_found` für `heuermannplus-registration-service` loggt.
- In diesem Fall den Stack für einen kompletten lokalen Neuimport einmal mit gelöschten Volumes starten:

```bash
docker compose down -v
docker compose up --build
```

## Entwicklung ohne Docker

- Frontend: Node.js 24.x LTS und `npm install && npm run dev` in `frontend/`
- Backend: Java 25 und `./gradlew bootRun` in `backend/`

Die Root-Compose-Datei bleibt trotzdem der bevorzugte lokale Einstieg, weil alle Abhängigkeiten damit konsistent hochfahren.

## Dev Container

Ein VS-Code-Dev-Container liegt unter `.devcontainer/` und ist auf diesen Repo-Stack zugeschnitten.

- Vorinstalliert: Node.js 24.x, npm, Java 25, Docker-CLI-Zugriff auf den Host-Daemon und `@openai/codex`
- Beim ersten Container-Start werden `frontend`-Dependencies installiert und der Gradle Wrapper in `backend` vorgewärmt
- Der Root-Stack aus `docker-compose.yml` startet nicht automatisch und bleibt bewusst manuell

Nutzung:

1. In VS Code `Dev Containers: Reopen in Container` ausführen.
2. Nach dem ersten Bootstrap bei Bedarf den App-Stack manuell starten:

```bash
docker compose up -d
```

3. Codex im Container manuell authentifizieren:

```bash
codex login
```

Alternativ kann ein API-Key wie `OPENAI_API_KEY` manuell im Container gesetzt werden. Die Dev-Container-Konfiguration reicht keine OpenAI-Credentials automatisch durch.
