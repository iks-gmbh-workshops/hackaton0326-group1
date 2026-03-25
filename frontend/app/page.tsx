import { getServerSession } from "next-auth";
import type { Route } from "next";
import Link from "next/link";
import { AuthControls } from "@/components/auth-controls";
import { ProtectedApiDemo } from "@/components/protected-api-demo";
import { authOptions } from "@/lib/auth";

export const dynamic = "force-dynamic";

const highlights = [
  "Mobile-first Layout mit Next.js App Router",
  "DaisyUI-Komponenten auf Tailwind CSS 4",
  "Keycloak Login ueber serverseitige Session",
  "Spring Boot Resource Server mit JWT-Validierung",
  "App-eigene Registrierung mit E-Mail-Verifizierung"
];

const steps = [
  "Neue Nutzer registrieren sich ueber das Frontend und bestaetigen ihre E-Mail.",
  "Keycloak verwaltet die Identitaet und Rollen fuer verifizierte Nutzer.",
  "next-auth speichert die Session im Frontend.",
  "Der Route-Handler `/api/me` fungiert als BFF zum Backend."
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
              <span className="badge badge-primary badge-outline">Scaffold</span>
            </div>

            <div className="max-w-3xl space-y-4">
              <p className="subheadline">Responsives Frontend fuer Authentifizierung, Registrierung und BFF-Zugriffe.</p>
              <h1 className="headline">
                Multilayer-Web-App mit klarer Trennung zwischen UI, API und IAM.
              </h1>
              <p className="body-copy max-w-2xl">
                Dieses Grundgeruest verbindet ein responsives Next.js-Frontend, ein Kotlin/Spring-Boot-Backend
                und Keycloak als Identity-Layer. Die lokale Entwicklung startet komplett ueber Docker Compose.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <AuthControls authenticated={authenticated} />
              {!authenticated ? (
                <Link className="btn btn-outline btn-primary" href={"/register" as Route}>
                  Jetzt registrieren
                </Link>
              ) : (
                <Link className="btn btn-outline btn-primary" href={"/groups" as Route}>
                  Zu den Gruppen
                </Link>
              )}
              <a className="btn btn-ghost" href="#architecture">
                Architektur ansehen
              </a>
            </div>
          </div>

          <div className="soft-panel grid gap-5">
            <div className="flex items-center justify-between">
              <p className="section-title">Session Status</p>
              <span className={`badge ${authenticated ? "badge-success" : "badge-neutral"} badge-lg`}>
                {authenticated ? "aktiv" : "inaktiv"}
              </span>
            </div>

            <div className="space-y-2">
              <h2 className="section-headline text-3xl">
                {authenticated ? `Willkommen ${session?.user?.name ?? "zurueck"}` : "Bereit fuer den ersten Login"}
              </h2>
              <p className="subheadline">
                {authenticated
                  ? "Die Session liegt serverseitig vor und kann jetzt ueber den BFF sicher an das Backend weitergegeben werden."
                  : "Registriere dich oder starte den Login ueber Keycloak, um den geschuetzten Backend-Endpoint live aus dem Frontend aufzurufen."}
              </p>
            </div>

            <div className="rounded-2xl border border-base-300 bg-white/90 p-4 text-sm text-base-content">
              <p className="subsection-title">Aktueller Benutzer</p>
              <p>{session?.user?.email ?? "Nicht angemeldet"}</p>
            </div>

            <div className="brand-divider" />

            <div className="status-list">
              <div className="status-row">
                <span className="status-dot" />
                <p className="body-copy text-sm">Linksbuendige Informationsbloecke sorgen fuer klare Lesefuehrung.</p>
              </div>
              <div className="status-row">
                <span className="status-dot" />
                <p className="body-copy text-sm">DaisyUI-Komponenten bleiben erhalten und werden nur gestalterisch neu gefasst.</p>
              </div>
            </div>
          </div>
        </section>

        <section className="page-section">
          <div className="section-intro">
            <p className="section-title">Schwerpunkte</p>
            <h2 className="section-headline">Was das Grundgeruest heute schon abdeckt</h2>
            <p className="subheadline">
              Die wichtigsten Funktionsbausteine sind in eigenstaendige Themenbloecke gegliedert und auf Wiederverwendbarkeit ausgelegt.
            </p>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {highlights.map((highlight) => (
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

        <section className="grid gap-6 xl:grid-cols-[1.08fr_0.92fr]" id="architecture">
          <ProtectedApiDemo authenticated={authenticated} />

          <div className="brand-card card">
            <div className="card-body gap-5">
              <div className="section-intro">
                <p className="section-title">Flow</p>
                <h2 className="section-headline">Wie die Demo zusammenspielt</h2>
                <p className="subheadline">
                  Die Architektur bleibt sichtbar getrennt, damit Rollen, Session und Backend-Aufrufe nachvollziehbar bleiben.
                </p>
              </div>

              <div className="info-grid">
                {steps.map((step, index) => (
                  <div key={step} className="step-row">
                    <span className="step-index">{index + 1}</span>
                    <p className="body-copy text-sm">{step}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
