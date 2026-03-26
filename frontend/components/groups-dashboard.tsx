"use client";

import Link from "next/link";
import type { Route } from "next";
import { useRouter } from "next/navigation";
import { useEffect, useState, useTransition } from "react";
import { authenticatedBackendFetch } from "@/lib/authenticated-backend-client";
import { formatDate, membershipLabel, type GroupDetail, type GroupError, type GroupListResponse } from "@/lib/group";

const initialList: GroupListResponse = {
  groups: [],
  invitations: [],
  joinRequests: [],
  availableGroups: []
};

export function GroupsDashboard() {
  const router = useRouter();
  const [data, setData] = useState<GroupListResponse>(initialList);
  const [createName, setCreateName] = useState("");
  const [createDescription, setCreateDescription] = useState("");
  const [joinToken, setJoinToken] = useState("");
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<GroupError | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  async function loadData() {
    const response = await authenticatedBackendFetch("/api/private/groups", { cache: "no-store" });
    const body = (await response.json()) as GroupListResponse & GroupError;

    if (!response.ok) {
      throw new Error(body.message || "Gruppen konnten nicht geladen werden");
    }

    setData(body);
    setLoadError(null);
  }

  useEffect(() => {
    void loadData().catch((error) => {
      setLoadError(error instanceof Error ? error.message : "Gruppen konnten nicht geladen werden");
    });
  }, []);

  function handleCreateGroup(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitError(null);
    setSuccessMessage(null);

    startTransition(() => {
      void authenticatedBackendFetch("/api/private/groups", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          name: createName,
          description: createDescription
        })
      })
        .then(async (response) => {
          const body = (await response.json()) as GroupDetail & GroupError;
          if (!response.ok) {
            setSubmitError({ code: body.code, message: body.message, field: body.field });
            return;
          }

          setCreateName("");
          setCreateDescription("");
          setSuccessMessage("Gruppe wurde angelegt");
          router.push(`/groups/${body.id}` as Route);
        })
        .catch(() => {
          setSubmitError({ code: "NETWORK_ERROR", message: "Gruppe konnte nicht angelegt werden" });
        });
    });
  }

  function handleJoinByToken(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitError(null);
    setSuccessMessage(null);

    startTransition(() => {
      void authenticatedBackendFetch("/api/private/groups/join-by-token", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token: joinToken })
      })
        .then(async (response) => {
          const body = (await response.json()) as GroupDetail & GroupError;
          if (!response.ok) {
            setSubmitError({ code: body.code, message: body.message, field: body.field });
            return;
          }

          setJoinToken("");
          setSuccessMessage("Du bist der Gruppe beigetreten");
          router.push(`/groups/${body.id}` as Route);
        })
        .catch(() => {
          setSubmitError({ code: "NETWORK_ERROR", message: "Token konnte nicht verarbeitet werden" });
        });
    });
  }

  function acceptInvitation(groupId: number, membershipId: number) {
    setSubmitError(null);
    setSuccessMessage(null);

    startTransition(() => {
      void authenticatedBackendFetch(`/api/private/groups/${groupId}/members/${membershipId}/accept`, { method: "POST" })
        .then(async (response) => {
          const body = (await response.json()) as GroupDetail & GroupError;
          if (!response.ok) {
            setSubmitError({ code: body.code, message: body.message, field: body.field });
            return;
          }

          setSuccessMessage("Einladung angenommen");
          await loadData();
        })
        .catch(() => {
          setSubmitError({ code: "NETWORK_ERROR", message: "Einladung konnte nicht angenommen werden" });
        });
    });
  }

  function requestMembership(groupId: number) {
    setSubmitError(null);
    setSuccessMessage(null);

    startTransition(() => {
      void authenticatedBackendFetch(`/api/private/groups/${groupId}/membership-requests`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({})
      })
        .then(async (response) => {
          const body = (await response.json()) as GroupListResponse & GroupError;
          if (!response.ok) {
            setSubmitError({ code: body.code, message: body.message, field: body.field });
            return;
          }

          setData(body);
          setSuccessMessage("Mitgliedschaft beantragt");
        })
        .catch(() => {
          setSubmitError({ code: "NETWORK_ERROR", message: "Mitgliedschaft konnte nicht beantragt werden" });
        });
    });
  }

  if (loadError) {
    return <div className="alert alert-error">{loadError}</div>;
  }

  return (
    <div className="grid gap-6 xl:grid-cols-[0.92fr_1.08fr]">
      <div className="space-y-6">
        <form className="brand-card space-y-4 p-6" onSubmit={handleCreateGroup}>
          <div className="section-intro">
            <p className="section-title">Neue Gruppe</p>
            <h1 className="section-headline">Gruppen anlegen und verwalten</h1>
            <p className="subheadline">
              Der Ersteller wird automatisch Gruppenverwalter und kann danach Mitglieder einladen.
            </p>
          </div>

          <Field
            label="Gruppenname"
            onChange={setCreateName}
            required
            value={createName}
          />
          <Field
            label="Beschreibung"
            onChange={setCreateDescription}
            value={createDescription}
          />

          {submitError?.field === "name" ? <div className="alert alert-error">{submitError.message}</div> : null}

          <div className="flex flex-wrap items-center gap-3">
            <button className="btn btn-primary" disabled={isPending} type="submit">
              {isPending ? "Speichere..." : "Gruppe erstellen"}
            </button>
            <Link className="btn btn-outline btn-primary" href={"/activities" as Route}>
              Meine Aktivitäten
            </Link>
            <Link className="btn btn-ghost" href={"/" as Route}>
              Zur Startseite
            </Link>
          </div>
        </form>

        <form className="soft-panel space-y-4" onSubmit={handleJoinByToken}>
          <div className="section-intro">
            <p className="section-title">Token</p>
            <h2 className="section-headline">Mit Einladungstoken beitreten</h2>
          </div>
          <Field
            label="Gruppeneinladungstoken"
            onChange={setJoinToken}
            required
            value={joinToken}
          />
          <button className="btn btn-outline btn-primary" disabled={isPending} type="submit">
            {isPending ? "Prüfe..." : "Token einlösen"}
          </button>
        </form>

        {submitError && !submitError.field ? <div className="alert alert-error">{submitError.message}</div> : null}
        {successMessage ? <div className="alert alert-success">{successMessage}</div> : null}
      </div>

      <div className="space-y-6">
        <section className="brand-card card">
          <div className="card-body gap-4">
            <div className="section-intro">
              <p className="section-title">Meine Gruppen</p>
              <h2 className="section-headline text-[2rem]">Aktive Zugehörigkeiten und Einladungen</h2>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              {data.groups.map((group) => (
                <article key={group.id} className="soft-panel space-y-3">
                  <div className="flex items-start justify-between gap-4">
                    <div>
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
            {!data.groups.length ? <p className="helper-text">Noch keine Gruppen vorhanden.</p> : null}
          </div>
        </section>

        <section className="soft-panel space-y-4">
          <div className="section-intro">
            <p className="section-title">Einladungen</p>
            <h2 className="section-headline">Offene Gruppeneinladungen</h2>
          </div>
          {data.invitations.map((invitation) => (
            <div key={invitation.membershipId} className="rounded-2xl border border-base-300 bg-white/85 p-4">
              <p className="subsection-title">{invitation.groupName}</p>
              <p className="helper-text">Eingeladen am {formatDate(invitation.invitedAt)}</p>
              <p className="body-copy mt-2 text-sm">Einladung für {invitation.displayName}</p>
              <div className="mt-3 flex flex-wrap gap-3">
                <button
                  className="btn btn-sm btn-primary"
                  disabled={isPending}
                  onClick={() => acceptInvitation(invitation.groupId, invitation.membershipId)}
                  type="button"
                >
                  Einladung annehmen
                </button>
                <Link className="btn btn-sm btn-ghost" href={`/groups/${invitation.groupId}` as Route}>
                  Detailseite
                </Link>
              </div>
            </div>
          ))}
          {!data.invitations.length ? <p className="helper-text">Keine offenen Einladungen.</p> : null}
        </section>

        <section className="soft-panel space-y-4">
          <div className="section-intro">
            <p className="section-title">Anträge</p>
            <h2 className="section-headline text-[2rem]">Eigene Mitgliedschaftsanträge</h2>
          </div>
          {data.joinRequests.map((request) => (
            <div key={request.id} className="rounded-2xl border border-base-300 bg-white/85 p-4">
              <p className="subsection-title">{request.groupName}</p>
              <p className="helper-text">{request.status.toLowerCase()}</p>
              {request.comment ? <p className="body-copy mt-2 text-sm">{request.comment}</p> : null}
            </div>
          ))}
          {!data.joinRequests.length ? <p className="helper-text">Keine offenen Anträge.</p> : null}
        </section>

        <section className="soft-panel space-y-4">
          <div className="section-intro">
            <p className="section-title">Gruppen finden</p>
            <h2 className="section-headline">Mitgliedschaft beantragen</h2>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            {data.availableGroups.map((group) => (
              <article key={group.id} className="rounded-2xl border border-base-300 bg-white/85 p-4">
                <p className="subsection-title">{group.name}</p>
                <p className="body-copy mt-2 text-sm">{group.description || "Keine Beschreibung hinterlegt."}</p>
                <p className="helper-text mt-2">{group.memberCount} aktive Mitglieder</p>
                <button
                  className="btn btn-sm btn-outline btn-primary mt-3"
                  disabled={isPending}
                  onClick={() => requestMembership(group.id)}
                  type="button"
                >
                  Mitgliedschaft beantragen
                </button>
              </article>
            ))}
          </div>
          {!data.availableGroups.length ? <p className="helper-text">Keine weiteren Gruppen verfügbar.</p> : null}
        </section>
      </div>
    </div>
  );
}

type FieldProps = {
  label: string;
  onChange: (value: string) => void;
  required?: boolean;
  value: string;
};

function Field({ label, onChange, required = false, value }: FieldProps) {
  return (
    <label className="form-control w-full gap-2">
      <span className="label-text font-medium">{label}</span>
      <input
        className="input input-bordered w-full"
        onChange={(event) => onChange(event.target.value)}
        required={required}
        value={value}
      />
    </label>
  );
}
