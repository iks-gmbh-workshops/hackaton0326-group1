# Theme bauen

```bash
  npm run build-keycloak-theme
```

Keycloakify generiert 2 .jar Dateien für verschiedene Versionen von Keycloak.
Wir benötigen aktuell "keycloak-theme-for-kc-all-other-versions.jar"

# Theme einbinden

Das Theme wird von der docker-compose.yml automatisch in das docker image REme-Keycloak hochgeladen.
Nachdem man sich in Keycloak als admin angemeldet hat, kann unter: Manage realms → reme → Realm settings → Themes → Login theme → reme-theme auswählen.

# Maven notwendig

Falls der Befehl sagt, dass Maven nicht gefunden wurde kann man dies entweder über die Windows Systemvariablen dauerhaft setzen,
oder über File -> Settings -> Tools -> Terminal Umgebungsvariablen setzen,
oder im Terminal temporär bevor der Befehl ausgeführt wird.
(Pfade entsprechend anpassen)

```bash
$env:PATH = "C:\Program Files (x86)\JetBrains\IntelliJ IDEA 2025.1.4.1\plugins\maven\lib\maven3\bin;$env:PATH"
$env:JAVA_HOME = "C:\Entwicklung\Java\jdk21.0.3_9_amazon_corretto"
```

