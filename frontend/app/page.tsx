import { getServerSession } from "next-auth";
import type { Route } from "next";
import Link from "next/link";
import { HomeDashboard } from "@/components/home-dashboard";
import { authOptions } from "@/lib/auth";

export const dynamic = "force-dynamic";

export default async function HomePage() {
  const session = await getServerSession(authOptions);
  const authenticated = Boolean(session?.user);
  const displayName = session?.user?.name ?? session?.user?.email ?? "Willkommen zurueck";

  if (authenticated) {
    return (
      <main className="page-shell">
        <HomeDashboard userName={displayName} />
      </main>
    );
  }

  return (
    <main className="page-shell">
      <div className="page-container">
        <section className="hero-panel">
          <div className="space-y-6">
            <div className="flex flex-wrap items-center gap-3">
              <div className="brand-kicker">HeuermannPlus</div>
              <span className="badge badge-primary badge-outline">Gruppen einfach organisieren</span>
            </div>

            <div className="max-w-3xl space-y-4">
              <p className="subheadline">Plane gemeinsam, koordiniere klar und behalte jederzeit den Ueberblick.</p>
              <h1 className="headline">
                Organisiere Gruppen und Aktivitaeten an einem Ort.
              </h1>
              <p className="body-copy max-w-2xl">
                HeuermannPlus hilft dir dabei, Gruppen aufzubauen, Mitglieder einzuladen und gemeinsame Vorhaben
                uebersichtlich zu koordinieren.
              </p>
              <p className="body-copy max-w-2xl">
                Vom ersten Beitritt bis zur laufenden Abstimmung bleibt der Einstieg einfach und die Organisation
                nachvollziehbar.
              </p>
            </div>

            <div className="space-y-3">
              <div className="flex flex-wrap items-center gap-3">
                <Link className="btn btn-primary btn-lg" href={"/register" as Route}>
                  Jetzt registrieren
                </Link>
              </div>
              <p className="helper-text max-w-xl">
                Erstelle dein Konto und starte direkt mit Gruppen, Einladungen und gemeinsamer Planung.
              </p>
            </div>
          </div>

          <div className="soft-panel grid gap-5">
            <div className="space-y-2">
              <h2 className="section-headline text-3xl">Schnell startklar</h2>
              <p className="subheadline">
                Registriere dich in wenigen Schritten und nutze danach alle wichtigen Funktionen fuer eure gemeinsame
                Organisation.
              </p>
            </div>

            <div className="rounded-2xl border border-base-300 bg-white/90 p-4 text-sm text-base-content">
              <p className="subsection-title">Was dich erwartet</p>
              <p className="body-copy text-sm">
                Ein zentraler Ort fuer Gruppenbeitritt, Einladungen und klare Abstimmung rund um gemeinsame
                Aktivitaeten.
              </p>
            </div>

            <div className="brand-divider" />

            <div className="status-list">
              <div className="status-row">
                <span className="status-dot" />
                <p className="body-copy text-sm">
                  Auf der Startseite findest du genau die zwei wichtigsten Einstiege: registrieren und anmelden.
                </p>
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
