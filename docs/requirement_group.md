In der Anwendung sollen Gruppen mit Benutzern verwaltet werden können.
Der Benutzer der eine Gruppe erstellt wird zum Gruppenverwalter.
Eine Gruppe soll später auch mehrere Gruppenverwalter haben dürfen.

Dazu gibt es folgende User Stories:

# 1 - Gruppe erstellen

Als angemeldeter Anwender möchte ich eine oder mehrere Gruppen erstellen.

## Formularfelder

Pflichtfelder: Gruppenname

Optional: Beschreibung

Extra: Erstelldatum

## Ablauf

Ein angemeldeter Anwender benutzt die Funktion "Gruppe erstellen".

Er füllt die Details zu dieser Gruppe aus und legt die Gruppe an.

Der Anwender wird damit automatisch zum Gruppenverwalter der Gruppe.

## Tests

### Positivtest

Daten werden wie gefordert eingegeben, die Gruppe wird angelegt und der User wird als Gruppenverwalter ausgewiesen.

### Negativtests

Pflichtfeld fehlt

Der Anwender lässt den Gruppennamen leer

System meldet: Bitte geben Sie einen Gruppennamen ein

Gruppenname existiert bereits

Der Anwender gibt einen bereits existierenden Gruppennamen ein

System meldet: Gruppenname bereits vergeben

# 2 - Gruppe verwalten

Als Gruppenverwalter möchte ich die Gruppe verwalten können.

## Formularfelder

Pflichtfelder: Gruppenname, Mitglieder

Optional: Beschreibung

Extra: Gruppe löschen

## Ablauf

Der Gruppenverwalter nutzt die Funktion “Gruppe verwalten”.

Der Gruppenverwalter kann den Namen oder die Beschreibung ändern.

Der Gruppenverwalter kann angemeldete oder anonyme Nutzer zur Gruppe hinzufügen oder entfernen.

## Tests

### Positivtest

### Negativtests

Pflichtfeld fehlt

# 3 - Gruppenmitglieder einladen

Als Gruppenverwalter möchte ich Anonymer Anwender und Angemeldete Anwender für meine Gruppe einladen.

## Formularfelder

Pflichtfelder: Nickname oder Email Adresse

Optional: 

Extra:

## Ablauf

Der Gruppenverwalter weist für die ausgewälte Gruppe Mitglieder zu

Für die Mitglieder trägt er einen drumdibum Nickname ein bzw. eine ihm bekannte E-Mail Adresse

Wenn der Nickname oder die Email Adresse bekannt ist, dann bekommt das Mitglied eine Information per Email geschickt, dass er zu einer neuen Gruppe eingeladen ist.

Ist der Empfänger bereits als Nutzer in der Anwendung bekannt, dann enthält diese E-Mail außerdem den Einladenden, den Gruppennamen, die Gruppenbeschreibung, die nächste Aktivität sowie zwei direkte Links zum Annehmen oder Ablehnen der Einladung.

Der Klick auf diese Links verarbeitet die Entscheidung direkt und führt danach auf eine Ergebnisseite mit den Gruppendaten und dem Status der Entscheidung.

Wird die Einladung per E-Mail abgelehnt, dann ist diese Ablehnung final und führt nicht mehr zu einem direkten Gruppenzugriff.

Wenn die Email Adresse nicht bekannt ist, dann bekommt das Mitglied eine Information per Email geschickt mit Informationen zu drumdibum und falls der Empfänger noch kein Mitglied ist, dann den Ablauf für Registration usw.

## Tests

### Positivtest

Daten werden wie gefordert eingegeben, 

die Mitglieder werden als "eingeladen" der Gruppe hinzugefügt.

falls der Nickname oder die Email Adresse bekannt ist, dann bekommt das Mitglied den zugehörigen Email Text

falls die Email-Adresse nicht bekannt ist, dann bekommt das Mitglied einen anderen Email Text

### Negativtests

Pflichtfeld fehlt

Mitglied existiert bereits

Der Anwender gibt einen bereits existierendes Mitglied ein.

## Alternativer Ablauf

Der Gruppenverwalter generiert ein Gruppeneinladungstoken.

Das angemeldete Mitglied gibt den Token ein und tritt der Gruppe bei. 

# 4 - Gruppeneinladungstoken eingeben

Als Mitglied möchte ich den Gruppeneinladungstoken eingeben können, nachdem ich mich in drumdibum eingeloggt habe, so dass ich zu jederzeit einer Gruppe beitreten kann ohne den Verifizierungslink nutzen zu müssen.

## Formularfelder

Pflichtfelder: Gruppenname

Optional: Beschreibung

Extra: Erstelldatum

## Ablauf

Das Mitglied erhält von seinem Gruppenverwalter den Gruppeneinladungstoken geschickt. Das Mitglied soll die Möglichkeit haben, dass dieser Gruppeneinladungstoken jederzeit innerhalb des eingeloggten Bereichs eingegeben werden kann.

Nach der Eingabe wird dem Anwender bestätigt, dass er der Gruppe beigetreten ist.

## Tests

### Positivtest

### Negativtests

# 5 - Als angemeldeter Anwender möchte ich die Mitgliedschaft in einer Gruppe beantragen

# 6 - Als Mitglied einer Gruppe möchte ich mich bei einer Gruppe abmelden

# 7 - Als Mitglied einer Gruppe möchte ich mir die Details der Gruppe ansehen.

# 8 - Als Gruppenverwalter möchte ich Mitglieder aus der Gruppe entfernen.

# 9 - Als Gruppenverwalter möchte ich eine existierende Gruppe auflösen.

# 10 - Als Gruppenverwalter möchte ich Mitgliedschaftsanträge bearbeiten.

# 11 - Als Gruppenverwalter möchte ich einem anderen Gruppenverwalter Rechte entziehen
