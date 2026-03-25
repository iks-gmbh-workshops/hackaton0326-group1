# Registrierung mit Rollen, Verifizierung und Captcha

## Zusammenfassung
- Die Registrierung wird als app-eigener Flow umgesetzt: öffentliche Registrierungsseite im Frontend, fachliche Prüfung und Verifizierung im Backend, Keycloak bleibt User- und Rollen-Store.
- Neue Nutzer starten im Status `registration-pending` und werden erst nach erfolgreicher E-Mail-Verifizierung zu `app-user` aktiviert.
- Gruppenrollen werden bei der Registrierung nicht vergeben: `group-member` und `group-admin` entstehen erst in späteren Gruppenprozessen, `app-admin` ist nicht selbst registrierbar.
- AGB sind in diesem Schnitt explizit noch nicht Teil des Flows; der Registrierungsprozess wird so aufgebaut, dass später ein versioniertes AGB-Feld ergänzt werden kann, ohne den Flow neu zu schneiden.

## Rollen- und Verhaltensmodell
- `Anonymer Anwender` ist ein nicht authentifizierter Zustand und keine Keycloak-Rolle.
- Keycloak-Realm bekommt die Rollen `registration-pending`, `app-user`, `group-member`, `group-admin`, `app-admin`; die bestehende Rolle `app-user` bleibt die Basisrolle fuer freigeschaltete Anwender.
- Bei `POST Registrierung` legt das Backend in Keycloak einen deaktivierten User an mit `username = Nickname`, separater `email`, optional `firstName` und `lastName`, Passwort gesetzt, Rolle `registration-pending`, `emailVerified = false`, `enabled = false`.
- Bei erfolgreicher Token-Pruefung schaltet das Backend den User frei: `enabled = true`, `emailVerified = true`, Rolle `registration-pending` entfernen, Rolle `app-user` setzen.
- Gruppen- und Admin-Rollen sind fuer diesen Flow nur modelliert, aber nicht ueber die Registrierung erreichbar.

## Implementierungsentscheidungen
- Frontend:
  - Neue oeffentliche Seite `/register` mit Pflichtfeldern `nickname`, `password`, `passwordRepeat`, `email`, `captcha` sowie optional `firstName`, `lastName`.
  - Live-Visualisierung der Passwortregeln direkt beim Tippen; Regeln kommen aus einer oeffentlichen Policy-API, damit Frontend, Backend und Keycloak nicht auseinanderlaufen.
  - Fehler werden feldbezogen angezeigt; fuer Nickname-Konflikte zeigt die UI den vom Backend gelieferten Vorschlag direkt zur Uebernahme an.
  - Verifizierungslink zeigt auf eine Frontend-Seite `/register/verify?token=...`; diese ruft das Backend auf und zeigt Erfolg oder fachlichen Fehler an. Kein Auto-Login, sondern Success-CTA zum normalen Sign-in.
- Backend:
  - Oeffentliche Endpunkte:
    - `GET /api/public/registration/policy`
    - `POST /api/public/registration`
    - `POST /api/public/registration/verify`
  - Request fuer Registrierung: `nickname`, `password`, `passwordRepeat`, `email`, `captchaToken`, optional `firstName`, `lastName`.
  - Response-Fehlermodell mit stabilen Codes und optionalen Feldern wie `field` und `suggestedNickname`; die UI rendert die vorgegebenen deutschen Meldungen aus der Story.
  - Validierungsreihenfolge: Pflichtfelder, E-Mail-Format, Passwort-Wiederholung, Passwortregeln, Captcha, Nickname/E-Mail-Eindeutigkeit, dann User-Anlage und Mailversand.
  - Nickname-Vorschlag: erster freier numerischer Suffix auf Basis des eingegebenen Nicknames, z. B. `nickname1`, `nickname2`, ...
  - Verifizierungstoken wird nicht in Keycloak, sondern in der App-DB verwaltet: Tabelle fuer Registrierungsverifizierungen mit `keycloakUserId`, `nickname`, `email`, `tokenHash`, `expiresAt`, `verifiedAt`, `status`, `createdAt`.
  - Token ist einmalig nutzbar und 24 Stunden gueltig; unbekannter Token liefert `INVALID_TOKEN`, abgelaufener Token liefert `TOKEN_EXPIRED`.
  - Vor erneuter Registrierung werden abgelaufene Pending-Registrierungen fuer dieselbe E-Mail oder denselben Nickname bereinigt; zusaetzlich gibt es einen Cleanup-Job fuer liegengebliebene, deaktivierte Pending-User.
  - Backend integriert Keycloak Admin API ueber einen eigenen technischen Client mit minimalen Rechten fuer User-Anlage, Rollenwechsel und Lookup; keine Nutzung des Bootstrap-Admin-Accounts.
  - Backend versendet die Verifizierungs-E-Mail selbst; fuer lokal/dev wird Mailpit in Docker Compose eingeplant.
  - Captcha wird ueber ein Provider-Interface angebunden, initial mit Cloudflare Turnstile; lokal/dev gibt es einen schaltbaren Mock-Modus.
- Keycloak:
  - `registrationAllowed` bleibt `false`, da die Registrierung fachlich in der App liegt.
  - Passwortpolicy wird im Realm gesetzt und muss der Policy-API entsprechen.
  - Login mit E-Mail bleibt erlaubt; da `username` nun der Nickname ist, koennen Nutzer spaeter mit Nickname oder E-Mail einsteigen.

## Oeffentliche Schnittstellen und Akzeptanz
- `GET /api/public/registration/policy` liefert die Mindestanforderungen fuer das Passwort und ob Captcha aktiv/mock ist.
- `POST /api/public/registration` liefert bei Erfolg `202 Accepted`; die UI bestaetigt, dass eine Verifizierungs-E-Mail versendet wurde.
- `POST /api/public/registration/verify` liefert bei Erfolg `200 OK`; die UI zeigt, dass der Anwender freigeschaltet wurde.
- Fehlerszenarien werden exakt abgebildet:
  - fehlender Nickname -> `Bitte Nickname eingeben`
  - ungueltige E-Mail -> `Ungueltiges Format der Email Adresse`
  - Nickname schon vorhanden -> `Nickname existiert bereits` plus `suggestedNickname`
  - Passwort-Wiederholung falsch -> `Passwort-Wiederholung falsch`
  - unsicheres Passwort -> `Passwort entspricht nicht den Mindestanforderungen`
  - Captcha falsch -> `Das Captcha wurde falsch eingegeben`
  - Token abgelaufen -> keine Freischaltung
  - Token unbekannt -> keine Freischaltung

## Testplan
- Backend-Tests:
  - Happy Path: Registrierung erzeugt deaktivierten Pending-User, schreibt Token, versendet Mail; Verifizierung aktiviert User und tauscht Rolle zu `app-user`.
  - Validierung: fehlende Pflichtfelder, E-Mail-Format, Passwort-Wiederholung, Passwortpolicy, Captcha-Fehler.
  - Konflikte: Nickname bereits vorhanden mit korrektem `suggestedNickname`; E-Mail bereits vorhanden als zusaetzlicher Guard.
  - Token-Faelle: ungueltig, abgelaufen, bereits verwendet.
  - Cleanup: abgelaufene Pending-Registrierung blockiert keine erneute Registrierung dauerhaft.
- Frontend-Tests:
  - Formularvalidierung und Passwortregel-Anzeige.
  - Darstellung der Backend-Fehler je Feld.
  - Uebernahme und Anzeige des Nickname-Vorschlags.
  - Erfolg/Fehler auf der Verifizierungsseite.
- Integrations-/E2E-Test:
  - Compose-basierter End-to-End-Flow mit Mailpit und Captcha-Mock von Registrierung bis Freischaltung.
  - Negativfaelle aus der Story mindestens einmal ueber den echten HTTP-Flow.

## Annahmen und Defaults
- Passwort-Default fuer V1: mindestens 8 Zeichen, je 1 Grossbuchstabe, Kleinbuchstabe, Ziffer und Sonderzeichen; wenn ihr spaeter andere Regeln wollt, wird nur die Policy zentral angepasst.
- AGB sind noch nicht implementiert, solange keine versionierte Fachvorgabe vorliegt.
- Erfolgsfall endet mit Freischaltung und Rueckmeldung an den Nutzer, nicht mit automatischem Login.
- Sprache der V1-Fehlermeldungen ist Deutsch, intern aber ueber stabile Fehlercodes modelliert.
