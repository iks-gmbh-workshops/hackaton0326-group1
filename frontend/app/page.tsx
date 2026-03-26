import { getServerSession } from "next-auth";
import type { Route } from "next";
import Link from "next/link";
import { HomeDashboard } from "@/components/home-dashboard";
import { PublicLoginButton } from "@/components/public-login-button";
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
        <section className="landing-hero">
          <div className="landing-hero-copy">
            <div className="landing-kicker-row">
              <span className="brand-kicker">HeuermannPlus</span>
              <span className="landing-eyebrow">Fuer Gruppen, die sich nicht im Chaos verlieren wollen</span>
            </div>

            <div className="section-intro">
              <p className="subheadline">Vom ersten Einladen bis zur gemeinsamen Aktivitaet bleibt alles an einem Ort.</p>
              <h1 className="display-headline">
                Organisiere Gruppen so, dass sich alle schneller zurechtfinden und leichter mitmachen.
              </h1>
              <p className="body-copy landing-lead">
                HeuermannPlus gibt Organisator:innen und Mitgliedern einen klaren gemeinsamen Ausgangspunkt: Gruppen
                anlegen, Menschen einladen, Aktivitaeten planen und den naechsten Schritt fuer alle sichtbar machen.
              </p>
            </div>

            <div className="landing-cta-row">
              <Link className="btn btn-primary btn-lg" href={"/register" as Route}>
                Konto erstellen
              </Link>
              <PublicLoginButton className="btn btn-ghost landing-login-button" />
            </div>

            <p className="helper-text landing-helper">
              Bereits dabei? Melde dich an und geh direkt zur bestehenden Gruppe oder zu deinen Aktivitaeten.
            </p>
          </div>

          <div className="landing-visual" aria-hidden="true">
            <div className="landing-visual-card landing-visual-card-primary">
              <p className="section-title">Heute im Blick</p>
              <div className="landing-visual-cluster">
                <div className="landing-token landing-token-wide">
                  <span className="landing-token-title">Sommergruppe</span>
                  <span className="landing-token-copy">Mitglieder einladen</span>
                </div>
                <div className="landing-token">
                  <span className="landing-token-title">Einladung</span>
                  <span className="landing-token-copy">geteilt</span>
                </div>
                <div className="landing-token landing-token-accent">
                  <span className="landing-token-title">Aktivitaet</span>
                  <span className="landing-token-copy">gemeinsam planen</span>
                </div>
              </div>
            </div>

            <div className="landing-visual-card landing-visual-card-secondary">
              <div className="landing-diagram">
                <span className="landing-node landing-node-large">Gruppe</span>
                <span className="landing-node">Mitglieder</span>
                <span className="landing-node">Einladungen</span>
                <span className="landing-node">Aktivitaeten</span>
              </div>
            </div>
          </div>
        </section>

        <section className="page-section landing-section">
          <div className="section-intro landing-section-intro">
            <p className="section-title">Fuer Organisator:innen und Mitglieder</p>
            <h2 className="section-headline">Beide Seiten sehen sofort, was als Naechstes wichtig ist.</h2>
          </div>

          <div className="landing-audience-grid">
            <article className="landing-story-card">
              <p className="subsection-title">Wenn du organisierst</p>
              <h3 className="headline landing-card-headline">Weniger Nachfragen. Mehr Klarheit fuer deine Gruppe.</h3>
              <p className="body-copy">
                Lege Gruppen an, lade Menschen ein und halte Aktivitaeten so fest, dass Entscheidungen nicht in
                einzelnen Nachrichten verloren gehen.
              </p>
            </article>

            <article className="landing-story-card landing-story-card-soft">
              <p className="subsection-title">Wenn du mitmachst</p>
              <h3 className="headline landing-card-headline">Du erkennst schneller, worum es geht und wie du teilnimmst.</h3>
              <p className="body-copy">
                Tritt einer Gruppe bei, sieh deine Aktivitaeten gebuendelt und bleib nah an dem, was fuer deine
                Teilnahme gerade relevant ist.
              </p>
            </article>
          </div>
        </section>

        <section className="page-section landing-section">
          <div className="section-intro landing-section-intro">
            <p className="section-title">So funktioniert der Einstieg</p>
            <h2 className="section-headline">Drei Schritte vom ersten Konto bis zur gemeinsamen Planung.</h2>
          </div>

          <div className="landing-steps-grid">
            <article className="landing-step-card">
              <span className="step-index">1</span>
              <p className="subsection-title">Ankommen</p>
              <p className="body-copy">
                Erstelle dein Konto und starte mit einem klaren Einstieg statt mit verstreuten Einzeltools.
              </p>
            </article>

            <article className="landing-step-card">
              <span className="step-index">2</span>
              <p className="subsection-title">Verbinden</p>
              <p className="body-copy">
                Erstelle eine Gruppe oder nimm eine Einladung an, damit alle am selben Ort zusammenkommen.
              </p>
            </article>

            <article className="landing-step-card">
              <span className="step-index">3</span>
              <p className="subsection-title">Koordinieren</p>
              <p className="body-copy">
                Plane Aktivitaeten, behalte Mitglieder im Blick und mache den naechsten Schritt fuer alle sichtbar.
              </p>
            </article>
          </div>
        </section>

        <section className="page-section landing-section">
          <div className="section-intro landing-section-intro">
            <p className="section-title">Warum HeuermannPlus</p>
            <h2 className="section-headline">Konkrete Produktstaerken fuer den Alltag deiner Gruppe.</h2>
          </div>

          <div className="landing-proof-grid">
            <article className="benefit-item landing-proof-card">
              <p className="subsection-title">Gruppen verwalten</p>
              <p className="body-copy">
                Gruppen erhalten einen festen Ort mit Namen, Beschreibung und klarer Verantwortung.
              </p>
            </article>

            <article className="benefit-item landing-proof-card">
              <p className="subsection-title">Mitglieder einladen</p>
              <p className="body-copy">
                Einladungen helfen dir, neue und bestehende Kontakte geordnet in die Gruppe zu holen.
              </p>
            </article>

            <article className="benefit-item landing-proof-card">
              <p className="subsection-title">Aktivitaeten planen</p>
              <p className="body-copy">
                Gemeinsame Aktivitaeten bleiben als konkrete Planung sichtbar statt nur als lose Absprache.
              </p>
            </article>

            <article className="benefit-item landing-proof-card">
              <p className="subsection-title">Mitmachen statt suchen</p>
              <p className="body-copy">
                Mitglieder sehen schneller, wo sie gehoeren, was ansteht und wie sie sich beteiligen koennen.
              </p>
            </article>
          </div>
        </section>

      </div>
    </main>
  );
}
