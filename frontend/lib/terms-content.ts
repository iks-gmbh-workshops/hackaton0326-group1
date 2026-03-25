export type TermsDocument = {
  slug: string;
  version: string;
  title: string;
  intro: string;
  sections: Array<{
    heading: string;
    paragraphs: string[];
  }>;
  updatedAtLabel: string;
};

const termsDocuments: Record<string, TermsDocument> = {
  "drumdibum-agb-2026-03": {
    slug: "drumdibum-agb-2026-03",
    version: "2026-03",
    title: "Nutzungsbedingungen fuer drumdibum",
    intro:
      "Diese Nutzungsbedingungen regeln die Registrierung und die Nutzung von drumdibum. Mit deiner Zustimmung bestaetigst du, dass du die aktuelle Version gelesen hast und sie fuer die Nutzung der Plattform akzeptierst.",
    updatedAtLabel: "Stand: Maerz 2026",
    sections: [
      {
        heading: "1. Geltungsbereich",
        paragraphs: [
          "drumdibum ist eine digitale Plattform zur Organisation gemeinsamer Aktivitaeten und Gruppen. Die Nutzungsbedingungen gelten fuer alle oeffentlichen und geschuetzten Bereiche der Anwendung.",
          "Mit dem Abschluss der Registrierung entsteht ein Nutzungsverhaeltnis zwischen dir und dem Betreiber von drumdibum."
        ]
      },
      {
        heading: "2. Registrierung und Zugang",
        paragraphs: [
          "Fuer die Nutzung ist eine vollstaendige und wahrheitsgemaesse Registrierung mit gueltiger E-Mail-Adresse erforderlich.",
          "Der Zugang wird erst freigeschaltet, nachdem die Registrierung bestaetigt und die Verifizierungs-E-Mail erfolgreich abgeschlossen wurde."
        ]
      },
      {
        heading: "3. Verantwortung fuer Kontodaten",
        paragraphs: [
          "Du bist dafuer verantwortlich, dein Passwort vertraulich zu behandeln und unbefugte Zugriffe auf dein Konto unverzueglich zu melden.",
          "Der Betreiber darf Konten sperren oder einschraenken, wenn ein Missbrauchsverdacht oder ein Verstoss gegen diese Bedingungen vorliegt."
        ]
      },
      {
        heading: "4. Zulaessige Nutzung",
        paragraphs: [
          "Die Plattform darf nur im Rahmen geltenden Rechts und der vorgesehenen Produktfunktionen verwendet werden.",
          "Inhalte oder Handlungen, die andere Benutzer schaedigen, den Betrieb stoeren oder gegen geltendes Recht verstossen, sind unzulaessig."
        ]
      },
      {
        heading: "5. Aenderungen der Bedingungen",
        paragraphs: [
          "Der Betreiber kann diese Nutzungsbedingungen fuer die Zukunft anpassen, wenn berechtigte Gruende vorliegen, etwa rechtliche, technische oder funktionale Aenderungen.",
          "Ueber wesentliche Aenderungen wirst du mit angemessener Frist informiert. Weitergehende Prozesse fuer kuenftige Zustimmungsformen werden produktseitig separat geregelt."
        ]
      }
    ]
  }
};

export function getTermsDocument(slug: string): TermsDocument | null {
  return termsDocuments[slug] ?? null;
}
