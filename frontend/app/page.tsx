import { getServerSession } from "next-auth";
import type { Route } from "next";
import Link from "next/link";
import { HomeDashboard } from "@/components/home-dashboard";
import { PublicLoginButton } from "@/components/public-login-button";
import { authOptions } from "@/lib/auth";

export const dynamic = "force-dynamic";

const metrics = [
  {
    title: "Einladungen geordnet",
    value: "01",
    copy: "Teile Gruppenbeitritte nachvollziehbar statt in verstreuten Nachrichten."
  },
  {
    title: "Aktivitäten gebündelt",
    value: "02",
    copy: "Plane Treffpunkte, Abläufe und nächste Schritte an einem gemeinsamen Ort."
  },
  {
    title: "Rollen klar lesbar",
    value: "03",
    copy: "Organisator:innen und Mitglieder sehen schneller, was für sie gerade relevant ist."
  }
];

const stages = [
  {
    step: "01",
    title: "Gruppe aufsetzen",
    copy: "Lege einen gemeinsamen Rahmen mit Name, Beschreibung und klarer Verantwortung fest."
  },
  {
    step: "02",
    title: "Menschen verbinden",
    copy: "Hole neue Mitglieder über Einladungen in denselben Arbeitskontext, ohne Informationsverlust."
  },
  {
    step: "03",
    title: "Aktivitäten steuern",
    copy: "Halte fest, was ansteht, wer beteiligt ist und was als Nächstes passieren soll."
  }
];

const proofPoints = [
  {
    title: "Für Organisator:innen",
    headline: "Für Organisator:innen: weniger Rückfragen und mehr Orientierung.",
    copy: "Die zentrale Gruppenansicht reduziert Abstimmungslast und zeigt, wo Entscheidungen oder Reaktionen gebraucht werden."
  },
  {
    title: "Für Mitglieder",
    headline: "Für Mitglieder: schneller verstehen und sicherer teilnehmen.",
    copy: "Mitglieder sehen Einladungen, Aktivitäten und den aktuellen Stand gebündelt statt verteilt über mehrere Kanäle."
  },
  {
    title: "Für den Einstieg",
    headline: "Für den Einstieg: ein klarer Start statt verteilter Tools.",
    copy: "Konto anlegen, verifizieren, loslegen: der Einstieg bleibt nachvollziehbar und führt direkt in die gemeinsame Struktur."
  },
  {
    title: "Für spätere Erweiterung",
    headline: "Für spätere Erweiterungen: die Oberfläche bleibt klar und aufnahmefähig.",
    copy: "Das Design priorisiert klare Sektionen und robuste Muster, damit künftige Funktionen nicht wie Fremdkörper wirken."
  }
];

export default async function HomePage() {
  const session = await getServerSession(authOptions);
  const authenticated = Boolean(session?.user);
  const displayName = session?.user?.name ?? session?.user?.email ?? "Willkommen zurück";

  if (authenticated) {
    return (
      <main className="page-shell">
        <HomeDashboard userName={displayName} />
      </main>
    );
  }

  return (
    <main className="page-shell">
      <div className="page-container public-shell">
        <section className="public-hero">
          <div className="public-hero-copy space-y-6">
            <div className="section-intro">
              <h1 className="display-headline">Gruppen, Einladungen und Aktivitäten an einem gemeinsamen Ort koordinieren.</h1>
              <p className="body-copy">
                HeuermannPlus strukturiert den Weg vom ersten Zugang bis zur laufenden Zusammenarbeit. Gruppen starten
                geordnet, Mitglieder finden schneller ihren Platz und Aktivitäten bleiben sichtbar statt implizit.
              </p>
            </div>

            <div className="public-auth-strip">
              <Link className="btn btn-primary btn-lg" href={"/register" as Route}>
                Konto erstellen
              </Link>
              <PublicLoginButton className="btn btn-outline btn-primary btn-lg" />
            </div>

            <p className="helper-text">
              Die erste Iteration konzentriert sich auf den öffentlichen Einstieg: klares Onboarding, stärkere
              Hierarchie und eine Shell, die später von den geschützten Bereichen weiterverwendet wird.
            </p>
          </div>

          <div className="public-stage-rail" aria-hidden="true">
            <div className="public-callout">
              <div className="section-intro">
                <h2 className="section-headline">So behältst du Gruppen, Einladungen und Aktivitäten im Blick.</h2>
              </div>

              <div className="public-stage-list">
                <div className="public-stage-row">
                  <span className="public-stage-number">A</span>
                  <div className="space-y-1">
                    <p className="subsection-title">Gruppenraum</p>
                    <p className="body-copy text-sm">Beschreibung, Mitgliedschaft und Verantwortung an einem Ort.</p>
                  </div>
                </div>

                <div className="public-stage-row">
                  <span className="public-stage-number">B</span>
                  <div className="space-y-1">
                    <p className="subsection-title">Einladungsfluss</p>
                    <p className="body-copy text-sm">Neue Personen gelangen nachvollziehbar in bestehende Gruppen.</p>
                  </div>
                </div>

                <div className="public-stage-row">
                  <span className="public-stage-number">C</span>
                  <div className="space-y-1">
                    <p className="subsection-title">Aktivitätsfokus</p>
                    <p className="body-copy text-sm">Nächste Schritte bleiben offen lesbar, auch wenn mehrere Personen beteiligt sind.</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="public-section page-section">
          <div className="section-intro">
            <h2 className="section-headline">Die Oberfläche bleibt nach Aufgaben geordnet und direkt verständlich.</h2>
            <p className="subheadline">
              Typografie, Farbcodierung und Flächenhierarchie folgen der Styleguide-Basis, während DaisyUI die
              Komponentenlogik für Buttons, Karten, Alerts und Formulare trägt.
            </p>
          </div>

          <div className="public-metric-grid">
            {metrics.map((metric) => (
              <article key={metric.title} className="public-metric-card">
                <p className="subsection-title">{metric.title}</p>
                <p className="public-metric-value">{metric.value}</p>
                <p className="body-copy text-sm">{metric.copy}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="public-section page-section">
          <div className="section-intro">
            <h2 className="section-headline">So führst du Gruppen vom Einstieg bis zur Planung.</h2>
          </div>

          <div className="public-stage-grid">
            {stages.map((stage) => (
              <article key={stage.step} className="public-stage-card">
                <span className="public-stage-number">{stage.step}</span>
                <p className="headline text-[1.35rem] leading-tight">{stage.title}</p>
                <p className="body-copy text-sm">{stage.copy}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="public-section page-section">
          <div className="section-intro">
            <h2 className="section-headline">Klare Orientierung für Organisator:innen und Mitglieder.</h2>
          </div>

          <div className="public-proof-grid">
            {proofPoints.map((point) => (
              <article key={point.title} className="feature-card">
                <h3 className="headline text-[1.45rem]">{point.headline}</h3>
                <p className="body-copy text-sm">{point.copy}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="public-callout page-section">
          <div className="section-intro">
            <h2 className="section-headline">Starte mit deinem Konto und führe deine Gruppe später in dieselbe Oberfläche.</h2>
            <p className="body-copy">
              Die jetzige Iteration verankert Navigation, Typografie, CTA-Hierarchie und Formularrhythmus. Die
              geschützten Bereiche können diese Muster im nächsten Schritt direkt übernehmen.
            </p>
          </div>

          <div className="public-auth-strip">
            <Link className="btn btn-primary" href={"/register" as Route}>
              Registrierung starten
            </Link>
            <PublicLoginButton className="btn btn-ghost" />
          </div>
        </section>
      </div>
    </main>
  );
}
