In der Anwendung sollen innerhalb von Gruppen Aktivitäten verwaltet werden können.



Dazu gibt es folgende User Stories:

# 1 - Aktivität erstellen

Als Gruppenverwalter möchte ich eine Aktivität für eine Gruppe erstellen, um die Aktivität zu planen

## Formularfelder

Pflichtfelder: Gruppe, Beschreibung, Datum, Uhrzeit, Ort

Optional: Freitext für Informationen zur Aktivität

Extra:

## Ablauf

Der Gruppenverwalter erstellt die Aktivität für eine Gruppe und speichert diese ab.

Der Status der Teilnahme jedes Mitglieds der Gruppe ist "offen".

## Tests

### Positivtest

Die Aktivität wird mit gültigen Werten erfasst und das System speichert die Aktivität sowie die aktuellen Mitglieder mit Status "offen".

### Negativtests

Pflichtfeld fehlt

Es wurde keine Beschreibung eingegeben

System meldet: Bitte Beschreibung eingeben

Aktivität wird nicht gespeichert

# 2 - Als Gruppenverwalter möchte ich weitere Mitglieder der Gruppe einer Aktivität zuweisen.

# 3 - Als Gruppenverwalter möchte ich darüber informiert werden, welches Mitglied an der Aktivität teilnimmt.

# 4 - Als Gruppenverwalter möchte ich die Aktivität terminlich verschieben. 

# 5 - Als Grupperverwalter möchte ich die Aktivität löschen.

# 6 - Aktivität annehmen bzw. ablehnen

Als Mitglied einer Aktivität möchte ich mich zu dieser Aktivität anmelden bzw. die Aktivität ablehnen.

## Formularfelder

Pflichtfelder:  

Optional: Freitextfeld

Extra:

## Ablauf

Das Mitglied bekommt die Details einer Aktivität angezeigt

Das Mitglied sagt zu bzw. ab. Das Mitglied kann außerdem mit "weiß ich noch nicht" signalisieren, dass es den Termin wahrgenommen hat, aber noch keine Aussage treffen kann.

In dem Freitextfeld kann das Mitglied eine Nachricht zu seinem Eintrag hinterlassen.

## Tests

## Positivtest

Das Mitglied sagt zu. Die Aktivität wird mit dem Status "zugesagt" für das Mitglied gespeichert

Das Mitglied sagt ab. Die Aktivität wird mit dem Status "abgesagt" für das Mitglied gespeichert

Das Mitglied teilt "weiß ich noch nicht" mit. Die Aktivtät wird mit dem Status "weiß ich noch nicht" für das Mitglied gespeichert.

Das Mitglied hinterläßt einen Freitext. Dieser Freitext wird für das Mitglied für die Aktivität gespeichert.

## Negativtests

# 7 - Als Mitglied einer Aktivität möchte ich über die neue Aktivität informiert werden.

# 8 - Als Mitglied einer Aktivität möchte ich über die neue Aktivität informiert werden.

Ein Mitglied bekommt alle Aktivitäten aufgelistet, die in der nächsten Zukunft anstehen.

Das Mitglied erhält visuell einen Hinweis, in welchem Status jede Aktivität in der Liste ist (offen, zugesagt, abgesagt, weiß ich noch nicht)

# 9 - Als Gruppenverwalter möchte ich eine Aktivität konfigurieren können, ob ein Mitglied explizit zusagen oder absagen muss.

# 10 - Als Mitglied einer Aktivität möchte ich zu einem bestimmten Zeitpunkt an die Aktivität errinnert werden.

# 11 - Als Mitglied einer Aktivität möchte ich zu einem bestimmten Zeitpunkt errinnert werden, dass ich mich noch entscheiden muss, sofern ich dies noch nicht getan habe.

# 12 - Als Gruppenverwalter möchte ich eine Liste aller Mitglieder einer Aktivität sehen, die anzeigt, ob die Mitglieder zugesagt, abgesagt oder sich noch nicht entschieden haben.

# 13 - Als Gruppenverwalter möchte ich x Tage vor der Aktivität eine Nachricht über den Status der An- und Abmeldungen bekommen.

# 14 - Als Gruppenverwalter möchte ich einer Aktivität Mitglieder zuweisen können, die im Falle von Absagen anderer Mitglieder nachträglich eingeladen werden.

# 15 - Als Gruppenverwalter möchte ich Mitgliederzuweisungen zu einer Aktivität aufheben.

# 16 - Als Gruppenverwalter möchte ich einer Aktivität einzelne Mitglieder zuweisen können.

# 17 - Als Gruppenverwalter möchte ich einzelne Mitglieder aus einer Aktivität entfernen können.

# 18 - Als Gruppenverwalter möchte ich, dass ein Mitglied nur einmal zu einer Aktivität hinzugefügt werden kann, so dass das Mitglied nicht mehrfach zusagen muss.
