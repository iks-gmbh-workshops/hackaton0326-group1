import { getServerSession } from "next-auth";
import type { Route } from "next";
import Link from "next/link";
import { AuthControls } from "@/components/auth-controls";
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
              <p className="subheadline">Planen, koordinieren und gemeinsam den Ueberblick behalten.</p>
              <h1 className="headline">
                HeuermannPlus bringt Gruppen und Aktivitaeten an einen Ort.
              </h1>
              <p className="body-copy max-w-2xl">
                Die Plattform hilft dabei, Gruppen zu organisieren, Mitglieder zusammenzubringen und gemeinsame
                Aktivitaeten klar und uebersichtlich zu koordinieren.
              </p>
              <p className="body-copy max-w-2xl">
                Vom ersten Beitritt bis zur laufenden Abstimmung bleibt der Einstieg bewusst einfach und auf das
                Wesentliche reduziert.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <AuthControls authenticated={authenticated} />
              <Link className="btn btn-outline btn-primary" href={"/register" as Route}>
                Jetzt registrieren
              </Link>
            </div>
          </div>

          <div className="soft-panel grid gap-5">
            <div className="space-y-2">
              <h2 className="section-headline text-3xl">Der schnelle Einstieg fuer neue Mitglieder</h2>
              <p className="subheadline">
                Registriere dich in wenigen Schritten oder melde dich direkt an, um Gruppen zu erstellen,
                Einladungen zu verwalten und Aktivitaeten zu koordinieren.
              </p>
            </div>

            <div className="rounded-2xl border border-base-300 bg-white/90 p-4 text-sm text-base-content">
              <p className="subsection-title">Was dich erwartet</p>
              <p className="body-copy text-sm">
                Ein zentraler Ort fuer Gruppenbeitritt, gemeinsame Planung und klare Kommunikation rund um eure
                Aktivitaeten.
              </p>
            </div>

            <div className="brand-divider" />

            <div className="status-list">
              <div className="status-row">
                <span className="status-dot" />
                <p className="body-copy text-sm">
                  Die Startseite zeigt nur noch den Produktkern und die zwei wichtigsten Einstiege: Anmelden und
                  Registrieren.
                </p>
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
