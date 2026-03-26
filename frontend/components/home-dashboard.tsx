"use client";

import Link from "next/link";
import type { Route } from "next";
import { useEffect, useState } from "react";
import { authenticatedBackendFetch } from "@/lib/authenticated-backend-client";
import { formatDate, membershipLabel, type GroupError, type GroupListResponse } from "@/lib/group";

type HomeDashboardProps = {
  userName: string;
};

const initialData: GroupListResponse = {
  groups: [],
  invitations: [],
  joinRequests: [],
  availableGroups: []
};

export function HomeDashboard({ userName }: HomeDashboardProps) {
  const [data, setData] = useState<GroupListResponse>(initialData);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    async function loadDashboard() {
      try {
        const response = await authenticatedBackendFetch("/api/private/groups", { cache: "no-store" });
        const body = (await response.json()) as GroupListResponse & GroupError;

        if (!response.ok) {
          throw new Error(body.message || "Dashboard konnte nicht geladen werden");
        }

        setData(body);
        setLoadError(null);
      } catch (error) {
        setLoadError(error instanceof Error ? error.message : "Dashboard konnte nicht geladen werden");
      } finally {
        setIsLoading(false);
      }
    }

    void loadDashboard();
  }, []);

  const groups = [...data.groups].sort((left, right) => left.name.localeCompare(right.name, "de"));
  const featuredGroups = groups.slice(0, 3);
  const remainingGroups = Math.max(groups.length - featuredGroups.length, 0);
  const statsGroups = isLoading ? "..." : String(groups.length);
  const statsInvitations = isLoading ? "..." : String(data.invitations.length);
  const statsRequests = isLoading ? "..." : String(data.joinRequests.length);

  return (
    <div className="page-container max-w-7xl">
      <section className="hero-panel">
        <div className="space-y-6">
          <div className="flex flex-wrap items-center gap-3">
            <div className="brand-kicker">Startseite</div>
            <span className="badge badge-primary badge-outline">Angemeldet</span>
          </div>

          <div className="max-w-3xl space-y-4">
            <p className="subheadline">Dein schneller Überblick für Gruppen und nächste Schritte.</p>
            <h1 className="headline">Willkommen zurück, {userName}.</h1>
            <p className="body-copy max-w-2xl">
              Hier siehst du auf einen Blick deine Gruppen, offene Einladungen und den Platz für kommende
              Aktivitäten.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Link className="btn btn-primary" href={"/groups" as Route}>
              Gruppen verwalten
            </Link>
          </div>
        </div>

        <div className="soft-panel grid gap-4">
          <div className="section-intro">
            <p className="section-title">Übersicht</p>
            <h2 className="section-headline text-3xl">Direkt nach dem Login orientieren</h2>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div className="rounded-2xl border border-base-300 bg-white/90 p-4">
              <p className="subsection-title">Meine Gruppen</p>
              <p className="section-headline mt-2 text-[2rem]">{statsGroups}</p>
              <p className="helper-text">aktive Zugehörigkeiten</p>
            </div>

            <div className="rounded-2xl border border-base-300 bg-white/90 p-4">
              <p className="subsection-title">Einladungen</p>
              <p className="section-headline mt-2 text-[2rem]">{statsInvitations}</p>
              <p className="helper-text">offene Gruppeneinladungen</p>
            </div>

            <div className="rounded-2xl border border-base-300 bg-white/90 p-4">
              <p className="subsection-title">Anträge</p>
              <p className="section-headline mt-2 text-[2rem]">{statsRequests}</p>
              <p className="helper-text">laufende Mitgliedschaftsanträge</p>
            </div>
          </div>

          <p className="body-copy text-sm">
            {isLoading
              ? "Die aktuellen Übersichten werden geladen."
              : data.invitations.length
              ? "Du hast offene Einladungen, die du in der Gruppenverwaltung direkt annehmen kannst."
              : "Keine offenen Einladungen im Moment."}
          </p>
        </div>
      </section>

      {loadError ? <div className="alert alert-error">{loadError}</div> : null}

      <section className="grid gap-6 xl:grid-cols-[1.08fr_0.92fr]">
        <section className="brand-card card">
          <div className="card-body gap-5">
            <div className="section-intro">
              <p className="section-title">Meine Gruppen</p>
              <h2 className="section-headline">Deine wichtigsten Gruppen auf einen Blick</h2>
              <p className="subheadline">
                Kompakt dargestellt für den schnellen Einstieg. Die komplette Verwaltung bleibt unter Gruppen.
              </p>
            </div>

            {isLoading ? <p className="helper-text">Gruppen werden geladen...</p> : null}

            {!isLoading && !loadError && !groups.length ? (
              <div className="soft-panel space-y-3">
                <p className="subsection-title">Noch keine Gruppe vorhanden</p>
                <p className="body-copy text-sm">
                  Erstelle deine erste Gruppe oder tritt mit einem Einladungstoken einer bestehenden Gruppe bei.
                </p>
                <div className="flex flex-wrap gap-3">
                  <Link className="btn btn-primary btn-sm" href={"/groups" as Route}>
                    Zu den Gruppen
                  </Link>
                </div>
              </div>
            ) : null}

            {!loadError && featuredGroups.length ? (
              <div className="grid gap-4 md:grid-cols-2">
                {featuredGroups.map((group) => (
                  <article key={group.id} className="soft-panel space-y-3">
                    <div className="flex items-start justify-between gap-4">
                      <div className="space-y-1">
                        <p className="subsection-title">{group.name}</p>
                        <p className="helper-text">{membershipLabel(group.membershipStatus)}</p>
                      </div>
                      {group.currentUserAdmin ? <span className="badge badge-primary badge-outline">Admin</span> : null}
                    </div>

                    <p className="body-copy text-sm">{group.description || "Keine Beschreibung hinterlegt."}</p>
                    <p className="helper-text">{group.memberCount} aktive Mitglieder</p>
                    <Link className="btn btn-sm btn-primary" href={`/groups/${group.id}` as Route}>
                      Details öffnen
                    </Link>
                  </article>
                ))}
              </div>
            ) : null}

            {!isLoading && !loadError && remainingGroups > 0 ? (
              <p className="helper-text">Und {remainingGroups} weitere Gruppen in der vollständigen Gruppenverwaltung.</p>
            ) : null}

            {!isLoading && !loadError && (data.invitations.length || data.joinRequests.length) ? (
              <div className="rounded-2xl border border-base-300 bg-white/90 p-4">
                <p className="subsection-title">Offene Punkte</p>
                <div className="mt-3 grid gap-3">
                  {data.invitations.slice(0, 2).map((invitation) => (
                    <div key={invitation.membershipId} className="flex items-start justify-between gap-4">
                      <div>
                        <p className="body-copy text-sm">{invitation.groupName}</p>
                        <p className="helper-text">Einladung vom {formatDate(invitation.invitedAt)}</p>
                      </div>
                      <span className="badge badge-outline">Einladung</span>
                    </div>
                  ))}

                  {data.joinRequests.slice(0, 2).map((request) => (
                    <div key={request.id} className="flex items-start justify-between gap-4">
                      <div>
                        <p className="body-copy text-sm">{request.groupName}</p>
                        <p className="helper-text">Antrag {request.status.toLowerCase()}</p>
                      </div>
                      <span className="badge badge-outline">Antrag</span>
                    </div>
                  ))}
                </div>
              </div>
            ) : null}

            <div className="flex flex-wrap items-center gap-3">
              <Link className="btn btn-outline btn-primary" href={"/groups" as Route}>
                Gesamte Gruppenverwaltung öffnen
              </Link>
            </div>
          </div>
        </section>

        <section className="soft-panel space-y-4">
          <div className="section-intro">
            <p className="section-title">Bevorstehende Aktivitäten</p>
            <h2 className="section-headline">Hier erscheinen deine nächsten Termine</h2>
            <p className="subheadline">
              Die Aktivitätenfunktion ist im Projekt noch nicht umgesetzt. Dieser Bereich ist dafür bereits vorgesehen.
            </p>
          </div>

          <div className="rounded-2xl border border-base-300 bg-white/90 p-5">
            <p className="subsection-title">Noch keine Aktivitätenansicht verfügbar</p>
            <p className="body-copy mt-3 text-sm">
              Sobald Aktivitäten im Produkt eingeführt sind, werden hier deine anstehenden Termine, Treffpunkte und
              relevante Aktualisierungen gesammelt angezeigt.
            </p>
          </div>

          <div className="status-list">
            <div className="status-row">
              <span className="status-dot" />
              <p className="body-copy text-sm">
                Der Bereich bleibt absichtlich ohne Platzhalterdaten, damit später echte Aktivitäten ohne
                Bedeutungsbruch eingebunden werden können.
              </p>
            </div>
          </div>
        </section>
      </section>
    </div>
  );
}
