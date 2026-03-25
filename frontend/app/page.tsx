import { getServerSession } from "next-auth";
import Link from "next/link";
import { AuthControls } from "@/components/auth-controls";
import { authOptions } from "@/lib/auth";

export const dynamic = "force-dynamic";

const featureHighlights = [
  "Gruppen erstellen und Verantwortlichkeiten klar organisieren.",
  "Mitglieder einladen, Rollen abstimmen und Zusammenarbeit strukturieren.",
  "Aktivitaeten planen, aktualisieren und als Liste im Blick behalten.",
  "Anmeldung, Registrierung und AGB-Akzeptanz in einem konsistenten Einstieg."
];

const productJourney = [
  "Konto anlegen und E-Mail bestaetigen.",
  "Erste Gruppe anlegen und Mitglieder einladen.",
  "Aktivitaeten planen, Listen pflegen und Termine abstimmen."
];

const focusAreas = [
  {
    title: "Gruppenverwaltung",
    description: "Lege Gruppen an, halte Zustaendigkeiten transparent und strukturiere gemeinsame Vorhaben an einem Ort."
  },
  {
    title: "Mitgliederverwaltung",
    description: "Lade neue Personen ein, begleite den Einstieg und schaffe Klarheit ueber Rollen und Beteiligung."
  },
  {
    title: "Aktivitaetenverwaltung",
    description: "Plane Treffen und Aufgaben mit klaren Listen, damit naechste Schritte fuer alle sichtbar bleiben."
  }
];

const authenticatedActions = [
  "Zur Anmeldung wechseln",
  "Gruppen und Aktivitaeten vorbereiten",
  "Mitgliedereinladungen organisieren"
];

const guestActions = [
  "Konto erstellen",
  "E-Mail bestaetigen",
  "Danach Gruppen und Aktivitaeten verwalten"
];

export default async function HomePage() {
  const session = await getServerSession(authOptions);
  const authenticated = Boolean(session?.user);

  return (
    <main className="page-shell">
      <div className="page-container">
        <section className="hero-panel">
          <div className="space-y-6">
            <div className="flex flex-wrap items-center gap-3">
              <div className="brand-kicker">HeuermannPlus</div>
              <span className="badge badge-primary badge-outline">Produktplattform</span>
            </div>

            <div className="max-w-3xl space-y-4">
              <p className="subheadline">Organisation fuer Gruppen, Mitglieder und gemeinsame Aktivitaeten.</p>
              <h1 className="headline">Alles an einem Ort, damit gemeinsame Planung einfach vorankommt.</h1>
              <p className="body-copy max-w-2xl">
                HeuermannPlus unterstuetzt den Einstieg vom ersten Konto bis zur laufenden Gruppenarbeit. Nutzer
                koordinieren Einladungen, planen Aktivitaeten und behalten wichtige Listen in einer klaren
                Arbeitsoberflaeche im Blick.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <AuthControls authenticated={authenticated} />
              {!authenticated ? (
                <Link className="btn btn-outline btn-primary" href="/register">
                  Jetzt registrieren
                </Link>
              ) : null}
              <a className="btn btn-ghost" href="#produktfokus">
                Produktfokus ansehen
              </a>
            </div>
          </div>

          <div className="soft-panel grid gap-5">
            <div className="flex items-center justify-between">
              <p className="section-title">Zugang</p>
              <span className={`badge ${authenticated ? "badge-success" : "badge-neutral"} badge-lg`}>
                {authenticated ? "aktiv" : "inaktiv"}
              </span>
            </div>

            <div className="space-y-2">
              <h2 className="section-headline text-3xl">
                {authenticated ? `Willkommen ${session?.user?.name ?? "zurueck"}` : "Bereit fuer den Einstieg"}
              </h2>
              <p className="subheadline">
                {authenticated
                  ? "Dein Zugang ist aktiv. Jetzt kannst du die naechsten Gruppen, Mitglieder und Aktivitaeten vorbereiten."
                  : "Registriere dich oder melde dich an, um Gruppen aufzubauen, Mitglieder einzuladen und Aktivitaeten zu planen."}
              </p>
            </div>

            <div className="rounded-2xl border border-base-300 bg-white/90 p-4 text-sm text-base-content">
              <p className="subsection-title">Konto</p>
              <p>{session?.user?.email ?? "Noch kein aktiver Zugang"}</p>
            </div>

            <div className="brand-divider" />

            <div className="status-list">
              {(authenticated ? authenticatedActions : guestActions).map((item) => (
                <div key={item} className="status-row">
                  <span className="status-dot" />
                  <p className="body-copy text-sm">{item}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="page-section">
          <div className="section-intro">
            <p className="section-title">Schwerpunkte</p>
            <h2 className="section-headline">Die wichtigsten Funktionen fuer den produktiven Start</h2>
            <p className="subheadline">
              Der Einstieg konzentriert sich auf die Aufgaben, die Gruppen in ihrem Alltag wirklich brauchen.
            </p>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {featureHighlights.map((highlight) => (
              <article
                key={highlight}
                className="feature-card rounded-[1.5rem] border border-base-300 bg-white/92"
              >
                <p className="section-title">Feature</p>
                <p className="body-copy text-[0.98rem]">{highlight}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="grid gap-6 xl:grid-cols-[1.02fr_0.98fr]" id="produktfokus">
          <div className="brand-card card">
            <div className="card-body gap-5">
              <div className="section-intro">
                <p className="section-title">Ablauf</p>
                <h2 className="section-headline">Vom Einstieg zur laufenden Organisation</h2>
                <p className="subheadline">
                  Jede Phase baut auf dem letzten Schritt auf und fuehrt schnell in die eigentliche Produktnutzung.
                </p>
              </div>

              <div className="info-grid">
                {productJourney.map((step, index) => (
                  <div key={step} className="step-row">
                    <span className="step-index">{index + 1}</span>
                    <p className="body-copy text-sm">{step}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="soft-panel grid gap-5">
            <div className="section-intro">
              <p className="section-title">Produktfokus</p>
              <h2 className="section-headline">Worauf HeuermannPlus im Kern ausgelegt ist</h2>
              <p className="subheadline">
                Die Plattform unterstuetzt den kompletten Weg von der Registrierung bis zur gemeinsamen Planung.
              </p>
            </div>

            <div className="grid gap-4">
              {focusAreas.map((area) => (
                <div key={area.title} className="rounded-[1.35rem] border border-base-300 bg-white/90 p-5">
                  <p className="subsection-title">{area.title}</p>
                  <p className="body-copy mt-2 text-sm">{area.description}</p>
                </div>
              ))}
            </div>

            <div className="brand-divider" />

            <div className="flex flex-wrap items-center gap-3">
              {!authenticated ? (
                <Link className="btn btn-primary" href="/register">
                  Konto erstellen
                </Link>
              ) : null}
              <Link className="btn btn-outline btn-primary" href="/register">
                {authenticated ? "Weitere Person registrieren" : "Registrierung ansehen"}
              </Link>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
