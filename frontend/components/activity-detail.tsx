"use client";

import Link from "next/link";
import type { Route } from "next";
import { useCallback, useEffect, useMemo, useState, useTransition } from "react";
import { authenticatedBackendFetch } from "@/lib/authenticated-backend-client";
import {
  activityStatusBadgeClass,
  combineDateTimeToIso,
  formatActivityDateTime,
  formatActivityStatus,
  toDateInputValue,
  toTimeInputValue,
  type ActivityDetail,
  type ActivityError,
  type ActivityResponseStatus
} from "@/lib/activity";
import { type GroupDetail } from "@/lib/group";

type ActivityDetailProps = {
  groupId: number;
  activityId: number;
};

const responseOptions: ActivityResponseStatus[] = ["OPEN", "ACCEPTED", "DECLINED", "MAYBE"];

export function ActivityDetailView({ groupId, activityId }: ActivityDetailProps) {
  const [activity, setActivity] = useState<ActivityDetail | null>(null);
  const [group, setGroup] = useState<GroupDetail | null>(null);
  const [description, setDescription] = useState("");
  const [details, setDetails] = useState("");
  const [location, setLocation] = useState("");
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [responseStatus, setResponseStatus] = useState<ActivityResponseStatus>("OPEN");
  const [responseNote, setResponseNote] = useState("");
  const [selectedMembershipId, setSelectedMembershipId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  const loadData = useCallback(async () => {
    const [activityResponse, groupResponse] = await Promise.all([
      authenticatedBackendFetch(`/api/private/groups/${groupId}/activities/${activityId}`, { cache: "no-store" }),
      authenticatedBackendFetch(`/api/private/groups/${groupId}`, { cache: "no-store" })
    ]);

    const activityBody = (await activityResponse.json()) as ActivityDetail & ActivityError;
    const groupBody = (await groupResponse.json()) as GroupDetail & ActivityError;

    if (!activityResponse.ok) {
      throw new Error(activityBody.message || "Aktivität konnte nicht geladen werden");
    }
    if (!groupResponse.ok) {
      throw new Error(groupBody.message || "Gruppe konnte nicht geladen werden");
    }

    setActivity(activityBody);
    setGroup(groupBody);
    setDescription(activityBody.description);
    setDetails(activityBody.details ?? "");
    setLocation(activityBody.location);
    setDate(toDateInputValue(activityBody.scheduledAt));
    setTime(toTimeInputValue(activityBody.scheduledAt));
    setResponseStatus(activityBody.currentUserResponseStatus ?? "OPEN");
    setResponseNote(
      activityBody.participants.find((participant) => participant.id === activityBody.currentUserParticipantId)?.responseNote ?? ""
    );
    setError(null);
  }, [activityId, groupId]);

  useEffect(() => {
    void loadData().catch((loadError) => {
      setError(loadError instanceof Error ? loadError.message : "Aktivität konnte nicht geladen werden");
    });
  }, [loadData]);

  const availableMembers = useMemo(() => {
    if (!group || !activity) {
      return [];
    }

    const activeParticipantMembershipIds = new Set(
      activity.participants.filter((participant) => !participant.removedAt).map((participant) => participant.groupMembershipId)
    );

    return group.members.filter((member) => member.status === "ACTIVE" && !activeParticipantMembershipIds.has(member.id));
  }, [activity, group]);

  function performAction(
    action: () => Promise<Response>,
    successText: string,
    onSuccess?: (body: ActivityDetail | null) => void
  ) {
    setError(null);
    setSuccessMessage(null);

    startTransition(() => {
      void action()
        .then(async (response) => {
          const body = response.status === 204 ? null : ((await response.json()) as ActivityDetail & ActivityError);

          if (!response.ok) {
            setError(body?.message ?? "Aktion fehlgeschlagen");
            return;
          }

          if (body) {
            setActivity(body);
            setDescription(body.description);
            setDetails(body.details ?? "");
            setLocation(body.location);
            setDate(toDateInputValue(body.scheduledAt));
            setTime(toTimeInputValue(body.scheduledAt));
            setResponseStatus(body.currentUserResponseStatus ?? "OPEN");
            setResponseNote(
              body.participants.find((participant) => participant.id === body.currentUserParticipantId)?.responseNote ?? ""
            );
          }

          setSuccessMessage(successText);
          onSuccess?.(body);
        })
        .catch(() => {
          setError("Aktion konnte nicht ausgeführt werden");
        });
    });
  }

  if (error && !activity) {
    return <div className="alert alert-error">{error}</div>;
  }

  if (!activity || !group) {
    return <div className="soft-panel">Lade Aktivität...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap gap-3">
        <Link className="btn btn-ghost" href={`/groups/${groupId}` as Route}>
          Zur Gruppe
        </Link>
        <Link className="btn btn-outline btn-primary" href={"/activities" as Route}>
          Meine Aktivitäten
        </Link>
      </div>

      {error ? <div className="alert alert-error">{error}</div> : null}
      {successMessage ? <div className="alert alert-success">{successMessage}</div> : null}

      <section className="brand-card grid gap-6 p-6 lg:grid-cols-[1.02fr_0.98fr]">
        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault();
            performAction(
              () =>
                authenticatedBackendFetch(`/api/private/groups/${groupId}/activities/${activityId}`, {
                  method: "PUT",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify({
                    description,
                    details,
                    location,
                    scheduledAt: combineDateTimeToIso(date, time)
                  })
                }),
              "Aktivität aktualisiert"
            );
          }}
        >
          <div className="section-intro">
            <p className="section-title">{activity.groupName}</p>
            <h1 className="section-headline sm:text-[2.4rem]">{activity.description}</h1>
            <p className="subheadline">Aktualisiert am {formatActivityDateTime(activity.updatedAt)}</p>
          </div>

          <ActivityField disabled={!activity.currentUserCanManage} label="Beschreibung" onChange={setDescription} value={description} />
          <ActivityField disabled={!activity.currentUserCanManage} label="Ort" onChange={setLocation} value={location} />
          <div className="grid gap-4 md:grid-cols-2">
            <ActivityField disabled={!activity.currentUserCanManage} label="Datum" onChange={setDate} type="date" value={date} />
            <ActivityField disabled={!activity.currentUserCanManage} label="Uhrzeit" onChange={setTime} type="time" value={time} />
          </div>
          <ActivityTextarea disabled={!activity.currentUserCanManage} label="Informationen" onChange={setDetails} value={details} />

          {activity.currentUserCanManage ? (
            <div className="flex flex-wrap gap-3">
              <button className="btn btn-primary" disabled={isPending} type="submit">
                {isPending ? "Speichere..." : "Änderungen speichern"}
              </button>
              <button
                className="btn btn-outline btn-error"
                disabled={isPending}
                onClick={() =>
                  performAction(
                    () => authenticatedBackendFetch(`/api/private/groups/${groupId}/activities/${activityId}`, { method: "DELETE" }),
                    "Aktivität gelöscht",
                    () => {
                      window.location.assign(`/groups/${groupId}`);
                    }
                  )}
                type="button"
              >
                Aktivität löschen
              </button>
            </div>
          ) : null}
        </form>

        <div className="soft-panel space-y-4">
          <div className="section-intro">
            <p className="section-title">Status</p>
            <h2 className="section-headline text-[2rem]">Teilnahme und Rückmeldungen</h2>
          </div>

          <div className="flex flex-wrap gap-2">
            <span className="badge badge-info badge-outline">{activity.participantCounts.open} offen</span>
            <span className="badge badge-success badge-outline">{activity.participantCounts.accepted} zugesagt</span>
            <span className="badge badge-error badge-outline">{activity.participantCounts.declined} abgesagt</span>
            <span className="badge badge-warning badge-outline">{activity.participantCounts.maybe} vielleicht</span>
          </div>

          <p className="body-copy text-sm">Termin: {formatActivityDateTime(activity.scheduledAt)}</p>
          <p className="body-copy text-sm">Ort: {activity.location}</p>
          {activity.details ? <p className="body-copy text-sm">{activity.details}</p> : null}

          {activity.currentUserCanRespond ? (
            <form
              className="space-y-4"
              onSubmit={(event) => {
                event.preventDefault();
                performAction(
                  () =>
                    authenticatedBackendFetch(`/api/private/groups/${groupId}/activities/${activityId}/response`, {
                      method: "POST",
                      headers: { "Content-Type": "application/json" },
                      body: JSON.stringify({ responseStatus, responseNote })
                    }),
                  "Rückmeldung gespeichert"
                );
              }}
            >
              <label className="form-control gap-2">
                <span className="label-text font-medium">Dein Status</span>
                <select
                  className="select select-bordered"
                  onChange={(event) => setResponseStatus(event.target.value as ActivityResponseStatus)}
                  value={responseStatus}
                >
                  {responseOptions.map((option) => (
                    <option key={option} value={option}>
                      {formatActivityStatus(option)}
                    </option>
                  ))}
                </select>
              </label>
              <ActivityTextarea label="Notiz" onChange={setResponseNote} value={responseNote} />
              <button className="btn btn-primary" disabled={isPending} type="submit">
                {isPending ? "Speichere..." : "Rückmeldung senden"}
              </button>
            </form>
          ) : (
            <div className="rounded-2xl border border-base-300 bg-white/85 p-4">
              <p className="helper-text">Du bist dieser Aktivität derzeit nicht als Teilnehmer zugewiesen.</p>
            </div>
          )}
        </div>
      </section>

      {activity.currentUserCanManage ? (
        <section className="soft-panel space-y-4">
          <div className="section-intro">
            <p className="section-title">Teilnehmer</p>
            <h2 className="section-headline text-[2rem]">Aktive Gruppenmitglieder zuweisen</h2>
          </div>

          <div className="flex flex-col gap-3 md:flex-row">
            <select
              className="select select-bordered w-full md:max-w-md"
              onChange={(event) => setSelectedMembershipId(event.target.value)}
              value={selectedMembershipId}
            >
              <option value="">Mitglied auswählen</option>
              {availableMembers.map((member) => (
                <option key={member.id} value={member.id}>
                  {member.displayName}
                </option>
              ))}
            </select>
            <button
              className="btn btn-primary"
              disabled={isPending || !selectedMembershipId}
              onClick={() =>
                performAction(
                  () =>
                    authenticatedBackendFetch(`/api/private/groups/${groupId}/activities/${activityId}/participants`, {
                      method: "POST",
                      headers: { "Content-Type": "application/json" },
                      body: JSON.stringify({ groupMembershipId: Number(selectedMembershipId) })
                    }),
                  "Teilnehmer hinzugefügt",
                  () => setSelectedMembershipId("")
                )}
              type="button"
            >
              Teilnehmer hinzufügen
            </button>
          </div>
          {!availableMembers.length ? <p className="helper-text">Keine weiteren aktiven Gruppenmitglieder verfügbar.</p> : null}
        </section>
      ) : null}

      <section className="brand-card card">
        <div className="card-body gap-4">
          <div className="section-intro">
            <p className="section-title">Teilnehmerliste</p>
            <h2 className="section-headline text-[2rem]">Mitglieder und Antworten</h2>
          </div>

          <div className="space-y-4">
            {activity.participants.map((participant) => (
              <article key={participant.id} className="rounded-2xl border border-base-300 bg-white/88 p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="subsection-title">{participant.displayName}</p>
                    <p className="helper-text">
                      {participant.removedAt ? "entfernt" : formatActivityStatus(participant.responseStatus)}
                      {participant.inviteEmail ? ` - ${participant.inviteEmail}` : ""}
                    </p>
                    {participant.responseNote ? <p className="body-copy mt-2 text-sm">{participant.responseNote}</p> : null}
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <span className={`badge ${activityStatusBadgeClass(participant.removedAt ? null : participant.responseStatus)} badge-outline`}>
                      {participant.removedAt ? "entfernt" : formatActivityStatus(participant.responseStatus)}
                    </span>
                    {participant.admin ? <span className="badge badge-primary badge-outline">Admin</span> : null}
                    {activity.currentUserCanManage && !participant.removedAt ? (
                      <button
                        className="btn btn-xs btn-outline btn-error"
                        disabled={isPending}
                        onClick={() =>
                          performAction(
                            () =>
                              authenticatedBackendFetch(
                                `/api/private/groups/${groupId}/activities/${activityId}/participants/${participant.id}`,
                                { method: "DELETE" }
                              ),
                            "Teilnehmer entfernt"
                          )}
                        type="button"
                      >
                        Entfernen
                      </button>
                    ) : null}
                  </div>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}

type ActivityFieldProps = {
  disabled?: boolean;
  label: string;
  onChange: (value: string) => void;
  type?: string;
  value: string;
};

function ActivityField({ disabled = false, label, onChange, type = "text", value }: ActivityFieldProps) {
  return (
    <label className="form-control w-full gap-2">
      <span className="label-text font-medium">{label}</span>
      <input
        className="input input-bordered w-full"
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        type={type}
        value={value}
      />
    </label>
  );
}

type ActivityTextareaProps = {
  disabled?: boolean;
  label: string;
  onChange: (value: string) => void;
  value: string;
};

function ActivityTextarea({ disabled = false, label, onChange, value }: ActivityTextareaProps) {
  return (
    <label className="form-control w-full gap-2">
      <span className="label-text font-medium">{label}</span>
      <textarea
        className="textarea textarea-bordered min-h-28 w-full"
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        value={value}
      />
    </label>
  );
}
