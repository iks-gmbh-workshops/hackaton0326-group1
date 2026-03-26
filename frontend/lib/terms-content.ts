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
    title: "Nutzungsbedingungen für drumdibum",
    intro:
      "Diese Nutzungsbedingungen regeln die Registrierung und die Nutzung von drumdibum. Mit deiner Zustimmung bestätigst du, dass du die aktuelle Version gelesen hast und sie für die Nutzung der Plattform akzeptierst.",
    updatedAtLabel: "Stand: März 2026",
    sections: [
      {
        heading: "1. Geltungsbereich",
        paragraphs: [
          "drumdibum ist eine digitale Plattform zur Organisation gemeinsamer Aktivitäten und Gruppen. Die Nutzungsbedingungen gelten für alle öffentlichen und geschützten Bereiche der Anwendung.",
          "Mit dem Abschluss der Registrierung entsteht ein Nutzungsverhältnis zwischen dir und dem Betreiber von drumdibum."
        ]
      },
      {
        heading: "2. Registrierung und Zugang",
        paragraphs: [
          "Für die Nutzung ist eine vollständige und wahrheitsgemäße Registrierung mit gültiger E-Mail-Adresse erforderlich.",
          "Der Zugang wird erst freigeschaltet, nachdem die Registrierung bestätigt und die Verifizierungs-E-Mail erfolgreich abgeschlossen wurde."
        ]
      },
      {
        heading: "3. Verantwortung für Kontodaten",
        paragraphs: [
          "Du bist dafür verantwortlich, dein Passwort vertraulich zu behandeln und unbefugte Zugriffe auf dein Konto unverzüglich zu melden.",
          "Der Betreiber darf Konten sperren oder einschränken, wenn ein Missbrauchsverdacht oder ein Verstoß gegen diese Bedingungen vorliegt."
        ]
      },
      {
        heading: "4. Zulässige Nutzung",
        paragraphs: [
          "Die Plattform darf nur im Rahmen geltenden Rechts und der vorgesehenen Produktfunktionen verwendet werden.",
          "Inhalte oder Handlungen, die andere Benutzer schädigen, den Betrieb stören oder gegen geltendes Recht verstoßen, sind unzulässig."
        ]
      },
      {
        heading: "5. Änderungen der Bedingungen",
        paragraphs: [
          "Der Betreiber kann diese Nutzungsbedingungen für die Zukunft anpassen, wenn berechtigte Gründe vorliegen, etwa rechtliche, technische oder funktionale Änderungen.",
          "Über wesentliche Änderungen wirst du mit angemessener Frist informiert. Weitergehende Prozesse für künftige Zustimmungsformen werden produktseitig separat geregelt."
        ]
      }
    ]
  }
};

export function getTermsDocument(slug: string): TermsDocument | null {
  return termsDocuments[slug] ?? null;
}
