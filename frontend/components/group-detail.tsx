"use client";

import Link from "next/link";
import type { Route } from "next";
import type { RefObject } from "react";
import { useCallback, useEffect, useRef, useState, useTransition } from "react";
import { authenticatedBackendFetch } from "@/lib/authenticated-backend-client";
import {
  activityStatusBadgeClass,
  fetchGroupActivities,
  formatActivityDateTime,
  formatActivityStatus,
  type ActivityListResponse
} from "@/lib/activity";
import {
  fetchGroupInviteSuggestions,
  formatDate,
  membershipLabel,
  type GroupDetail,
  type GroupError,
  type GroupInviteSuggestion,
  type GroupListResponse,
  type GroupToken
} from "@/lib/group";

type GroupDetailProps = {
  groupId: number;
};

export function GroupDetailView({ groupId }: GroupDetailProps) {
  const [group, setGroup] = useState<GroupDetail | null>(null);
  const [activities, setActivities] = useState<ActivityListResponse>({ activities: [] });
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [inviteTarget, setInviteTarget] = useState("");
  const [inviteSuggestions, setInviteSuggestions] = useState<GroupInviteSuggestion[]>([]);
  const [inviteSuggestionsLoading, setInviteSuggestionsLoading] = useState(false);
  const [inviteSuggestionsOpen, setInviteSuggestionsOpen] = useState(false);
  const [inviteSuggestionsError, setInviteSuggestionsError] = useState<string | null>(null);
  const [latestToken, setLatestToken] = useState<GroupToken | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();
  const inviteFieldRef = useRef<HTMLDivElement | null>(null);
  const inviteSuggestionRequest = useRef(0);

  const loadGroup = useCallback(async () => {
    const response = await authenticatedBackendFetch(`/api/private/groups/${groupId}`, { cache: "no-store" });
    const body = (await response.json()) as GroupDetail & GroupError;

    if (!response.ok) {
      throw new Error(body.message || "Gruppe konnte nicht geladen werden");
    }

    setGroup(body);
    setName(body.name);
    setDescription(body.description ?? "");
    setError(null);
  }, [groupId]);

  useEffect(() => {
    void loadGroup().catch((loadError) => {
      setError(loadError instanceof Error ? loadError.message : "Gruppe konnte nicht geladen werden");
    });
  }, [loadGroup]);

  useEffect(() => {
    void fetchGroupActivities(groupId)
      .then((response) => {
        setActivities(response);
      })
      .catch(() => {
        setActivities({ activities: [] });
      });
  }, [groupId]);

  useEffect(() => {
    if (!group?.currentUserAdmin || !inviteSuggestionsOpen) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      const requestId = inviteSuggestionRequest.current + 1;
      inviteSuggestionRequest.current = requestId;
      setInviteSuggestionsLoading(true);
      setInviteSuggestionsError(null);

      void fetchGroupInviteSuggestions(groupId, inviteTarget)
        .then((suggestions) => {
          if (inviteSuggestionRequest.current !== requestId) {
            return;
          }
          setInviteSuggestions(suggestions);
        })
        .catch(() => {
          if (inviteSuggestionRequest.current !== requestId) {
            return;
          }
          setInviteSuggestions([]);
          setInviteSuggestionsError("Vorschlaege konnten nicht geladen werden");
        })
        .finally(() => {
          if (inviteSuggestionRequest.current === requestId) {
            setInviteSuggestionsLoading(false);
          }
        });
    }, inviteTarget.trim() ? 180 : 0);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [group?.currentUserAdmin, groupId, inviteSuggestionsOpen, inviteTarget]);

  useEffect(() => {
    if (!inviteSuggestionsOpen) {
      return;
    }

    function handlePointerDown(event: PointerEvent) {
      if (!inviteFieldRef.current?.contains(event.target as Node)) {
        setInviteSuggestionsOpen(false);
      }
    }

    window.addEventListener("pointerdown", handlePointerDown);
    return () => {
      window.removeEventListener("pointerdown", handlePointerDown);
    };
  }, [inviteSuggestionsOpen]);

  function performAction(
    action: () => Promise<Response>,
    onSuccess?: (body: GroupDetail | GroupListResponse | GroupToken) => void
  ) {
    setError(null);
    setSuccessMessage(null);

    startTransition(() => {
      void action()
        .then(async (response) => {
          const body = response.status === 204
            ? null
            : ((await response.json()) as GroupDetail & GroupError & GroupToken);

          if (!response.ok) {
            setError(body?.message ?? "Aktion fehlgeschlagen");
            return;
          }

          if (body && "name" in body && "members" in body) {
            setGroup(body);
            setName(body.name);
            setDescription(body.description ?? "");
          }

          onSuccess?.(body as GroupDetail | GroupListResponse | GroupToken);
        })
        .catch(() => {
          setError("Aktion konnte nicht ausgeführt werden");
        });
    });
  }

  if (error && !group) {
    return <div className="alert alert-error">{error}</div>;
  }

  if (!group) {
    return <div className="soft-panel">Lade Gruppe...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-3">
        <Link className="btn btn-ghost" href={"/groups" as Route}>
          Zur Gruppenübersicht
        </Link>
        <span className={`badge ${group.currentUserAdmin ? "badge-primary" : "badge-neutral"} badge-outline`}>
          {group.currentUserAdmin ? "Gruppenverwalter" : membershipLabel(group.currentMembershipStatus)}
        </span>
      </div>

      {error ? <div className="alert alert-error">{error}</div> : null}
      {successMessage ? <div className="alert alert-success">{successMessage}</div> : null}

      <section className="brand-card grid gap-6 p-6 lg:grid-cols-[1.05fr_0.95fr]">
        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault();
            performAction(
              () =>
                authenticatedBackendFetch(`/api/private/groups/${groupId}`, {
                  method: "PUT",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify({ name, description })
                }),
              () => setSuccessMessage("Gruppe aktualisiert")
            );
          }}
        >
          <div className="section-intro">
            <p className="section-title">Gruppendetails</p>
            <h1 className="section-headline">{group.name}</h1>
            <p className="subheadline">Erstellt am {formatDate(group.createdAt)}</p>
          </div>

          <div className="flex flex-wrap gap-3">
            <Link className="btn btn-outline btn-primary" href={"/activities" as Route}>
              Meine Aktivitaeten
            </Link>
            {group.currentUserAdmin ? (
              <Link className="btn btn-primary" href={`/groups/${groupId}/activities/new` as Route}>
                Aktivitaet erstellen
              </Link>
            ) : null}
          </div>

          <Field disabled={!group.currentUserAdmin} label="Gruppenname" onChange={setName} value={name} />
          <Field disabled={!group.currentUserAdmin} label="Beschreibung" onChange={setDescription} value={description} />

          {group.currentUserAdmin ? (
            <button className="btn btn-primary" disabled={isPending} type="submit">
              {isPending ? "Speichere..." : "Änderungen speichern"}
            </button>
          ) : null}
        </form>

        <div className="soft-panel space-y-4">
          <div className="section-intro">
            <p className="section-title">Mitgliedschaft</p>
            <h2 className="section-headline">Dein Status</h2>
          </div>
          <p className="body-copy text-sm">{membershipLabel(group.currentMembershipStatus)}</p>
          {group.currentMembershipStatus === "INVITED" && group.currentMembershipId ? (
            <button
              className="btn btn-primary"
              disabled={isPending}
              onClick={() =>
                performAction(
                  () => authenticatedBackendFetch(`/api/private/groups/${groupId}/members/${group.currentMembershipId}/accept`, { method: "POST" }),
                  () => setSuccessMessage("Einladung angenommen")
                )}
              type="button"
            >
              Einladung annehmen
            </button>
          ) : null}
          {group.currentMembershipStatus === "ACTIVE" ? (
            <button
              className="btn btn-outline btn-primary"
              disabled={isPending}
              onClick={() =>
                performAction(
                  () => authenticatedBackendFetch(`/api/private/groups/${groupId}/leave`, { method: "POST" }),
                  () => {
                    setSuccessMessage("Gruppe verlassen");
                    window.location.assign("/groups");
                  }
                )}
              type="button"
            >
              Gruppe verlassen
            </button>
          ) : null}
          {group.currentUserAdmin ? (
            <button
              className="btn btn-outline btn-error"
              disabled={isPending}
              onClick={() =>
                performAction(
                  () => authenticatedBackendFetch(`/api/private/groups/${groupId}`, { method: "DELETE" }),
                  () => {
                    setSuccessMessage("Gruppe aufgelöst");
                    window.location.assign("/groups");
                  }
                )}
              type="button"
            >
              Gruppe auflösen
            </button>
          ) : null}
        </div>
      </section>

      {group.currentUserAdmin ? (
        <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
          <form
            className="soft-panel space-y-4"
            onSubmit={(event) => {
              event.preventDefault();
              performAction(
                () =>
                  authenticatedBackendFetch(`/api/private/groups/${groupId}/members/invite`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ nicknameOrEmail: inviteTarget })
                  }),
                () => {
                  setInviteTarget("");
                  setInviteSuggestions([]);
                  setInviteSuggestionsError(null);
                  setInviteSuggestionsOpen(false);
                  setSuccessMessage("Einladung versendet");
                }
              );
            }}
          >
            <div className="section-intro">
              <p className="section-title">Einladen</p>
              <h2 className="section-headline">Mitglieder per Nickname oder E-Mail</h2>
            </div>
            <InviteSuggestionField
              label="Nickname oder E-Mail-Adresse"
              loading={inviteSuggestionsLoading}
              onChange={setInviteTarget}
              onFocus={() => setInviteSuggestionsOpen(true)}
              onSelectSuggestion={(suggestion) => {
                setInviteTarget(suggestion.nickname);
                setInviteSuggestionsOpen(false);
              }}
              open={inviteSuggestionsOpen}
              suggestions={inviteSuggestions}
              suggestionsError={inviteSuggestionsError}
              value={inviteTarget}
              wrapperRef={inviteFieldRef}
            />
            <button className="btn btn-primary" disabled={isPending} type="submit">
              {isPending ? "Sende..." : "Mitglied einladen"}
            </button>
          </form>

          <div className="soft-panel space-y-4">
            <div className="section-intro">
              <p className="section-title">Token</p>
              <h2 className="section-headline">Einladungstoken generieren</h2>
            </div>
            <button
              className="btn btn-outline btn-primary"
              disabled={isPending}
              onClick={() =>
                performAction(
                  () => authenticatedBackendFetch(`/api/private/groups/${groupId}/tokens`, { method: "POST" }),
                  (body) => {
                    setLatestToken(body as GroupToken);
                    setSuccessMessage("Token erstellt");
                    void loadGroup();
                  }
                )}
              type="button"
            >
              {isPending ? "Erzeuge..." : "Token erzeugen"}
            </button>
            {latestToken?.token ? (
              <div className="result-surface">
                <pre>{latestToken.token}</pre>
              </div>
            ) : null}
            <div className="space-y-3">
              {group.tokens.map((token) => (
                <div key={token.id} className="rounded-2xl border border-base-300 bg-white/85 p-4">
                  <p className="subsection-title">Token #{token.id}</p>
                  <p className="helper-text">Gültig bis {formatDate(token.expiresAt)}</p>
                  <p className="helper-text">{token.usedAt ? `Eingelöst am ${formatDate(token.usedAt)}` : "Noch nicht verwendet"}</p>
                </div>
              ))}
            </div>
          </div>
        </section>
      ) : null}

      <section className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
        <div className="brand-card card">
          <div className="card-body gap-4">
            <div className="section-intro">
              <p className="section-title">Aktivitäten</p>
              <h2 className="section-headline text-[2rem]">Anstehende Gruppentermine</h2>
            </div>

            <div className="space-y-4">
              {activities.activities.map((activity) => (
                <div key={activity.id} className="rounded-2xl border border-base-300 bg-white/88 p-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p className="subsection-title">{activity.description}</p>
                      <p className="helper-text">{formatActivityDateTime(activity.scheduledAt)}</p>
                      <p className="helper-text">{activity.location}</p>
                    </div>
                    <span className={`badge ${activityStatusBadgeClass(activity.currentUserResponseStatus)} badge-outline`}>
                      {formatActivityStatus(activity.currentUserResponseStatus)}
                    </span>
                  </div>

                  <div className="mt-3 flex flex-wrap gap-2 text-xs">
                    <span className="badge badge-info badge-outline">{activity.participantCounts.open} offen</span>
                    <span className="badge badge-success badge-outline">{activity.participantCounts.accepted} zugesagt</span>
                    <span className="badge badge-error badge-outline">{activity.participantCounts.declined} abgesagt</span>
                    <span className="badge badge-warning badge-outline">{activity.participantCounts.maybe} vielleicht</span>
                  </div>

                  <Link className="btn btn-sm btn-primary mt-3" href={`/groups/${groupId}/activities/${activity.id}` as Route}>
                    Aktivitaet ansehen
                  </Link>
                </div>
              ))}
            </div>
            {!activities.activities.length ? <p className="helper-text">Noch keine anstehenden Aktivitaeten.</p> : null}
          </div>
        </div>

        <div className="brand-card card">
          <div className="card-body gap-4">
            <div className="section-intro">
              <p className="section-title">Mitglieder</p>
              <h2 className="section-headline">Mitglieder und Rollen</h2>
            </div>

            <div className="space-y-4">
              {group.members.map((member) => (
                <div key={member.id} className="rounded-2xl border border-base-300 bg-white/88 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <p className="subsection-title">{member.displayName}</p>
                      <p className="helper-text">
                        {membershipLabel(member.status)}
                        {member.inviteEmail ? ` - ${member.inviteEmail}` : ""}
                      </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      {member.admin ? <span className="badge badge-primary badge-outline">Admin</span> : null}
                      {group.currentUserAdmin && member.status === "ACTIVE" && !member.admin ? (
                        <button
                          className="btn btn-xs btn-outline btn-primary"
                          disabled={isPending}
                          onClick={() =>
                            performAction(
                              () => authenticatedBackendFetch(`/api/private/groups/${groupId}/admins/${member.id}/grant`, { method: "POST" }),
                              () => setSuccessMessage("Adminrechte vergeben")
                            )}
                          type="button"
                        >
                          Admin machen
                        </button>
                      ) : null}
                      {group.currentUserAdmin && member.status === "ACTIVE" && member.admin ? (
                        <button
                          className="btn btn-xs btn-outline"
                          disabled={isPending}
                          onClick={() =>
                            performAction(
                              () => authenticatedBackendFetch(`/api/private/groups/${groupId}/admins/${member.id}/revoke`, { method: "POST" }),
                              () => setSuccessMessage("Adminrechte entzogen")
                            )}
                          type="button"
                        >
                          Admin entziehen
                        </button>
                      ) : null}
                      {group.currentUserAdmin ? (
                        <button
                          className="btn btn-xs btn-outline btn-error"
                          disabled={isPending}
                          onClick={() =>
                            performAction(
                              () => authenticatedBackendFetch(`/api/private/groups/${groupId}/members/${member.id}`, { method: "DELETE" }),
                              () => setSuccessMessage("Mitglied entfernt")
                            )}
                          type="button"
                        >
                          Entfernen
                        </button>
                      ) : null}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="space-y-6">
          {group.currentUserAdmin ? (
            <section className="soft-panel space-y-4">
              <div className="section-intro">
                <p className="section-title">Anträge</p>
                <h2 className="section-headline text-[2rem]">Offene Mitgliedschaftsanträge</h2>
              </div>
              {group.joinRequests.map((request) => (
                <div key={request.id} className="rounded-2xl border border-base-300 bg-white/85 p-4">
                  <p className="subsection-title">{request.requestedByDisplayName}</p>
                  {request.comment ? <p className="body-copy mt-2 text-sm">{request.comment}</p> : null}
                  <div className="mt-3 flex flex-wrap gap-3">
                    <button
                      className="btn btn-sm btn-primary"
                      disabled={isPending}
                      onClick={() =>
                        performAction(
                          () =>
                            authenticatedBackendFetch(`/api/private/groups/${groupId}/membership-requests/${request.id}/approve`, {
                              method: "POST",
                              headers: { "Content-Type": "application/json" },
                              body: JSON.stringify({})
                            }),
                          () => setSuccessMessage("Antrag genehmigt")
                        )}
                      type="button"
                    >
                      Genehmigen
                    </button>
                    <button
                      className="btn btn-sm btn-outline"
                      disabled={isPending}
                      onClick={() =>
                        performAction(
                          () =>
                            authenticatedBackendFetch(`/api/private/groups/${groupId}/membership-requests/${request.id}/reject`, {
                              method: "POST",
                              headers: { "Content-Type": "application/json" },
                              body: JSON.stringify({})
                            }),
                          () => setSuccessMessage("Antrag abgelehnt")
                        )}
                      type="button"
                    >
                      Ablehnen
                    </button>
                  </div>
                </div>
              ))}
              {!group.joinRequests.length ? <p className="helper-text">Keine offenen Anträge.</p> : null}
            </section>
          ) : null}

          <section className="soft-panel space-y-4">
            <div className="section-intro">
              <p className="section-title">Einladungen</p>
              <h2 className="section-headline">Versendete Einladungen</h2>
            </div>
            {group.invitations.map((invitation) => (
              <div key={invitation.id} className="rounded-2xl border border-base-300 bg-white/85 p-4">
                <p className="subsection-title">{invitation.targetLabel}</p>
                <p className="helper-text">
                  {invitation.channel.toLowerCase()} - {invitation.mailType.toLowerCase()}
                </p>
                <p className="helper-text">Erstellt am {formatDate(invitation.createdAt)}</p>
              </div>
            ))}
            {!group.invitations.length ? <p className="helper-text">Noch keine Einladungen.</p> : null}
          </section>
        </div>
      </section>
    </div>
  );
}

type FieldProps = {
  disabled?: boolean;
  label: string;
  onChange: (value: string) => void;
  value: string;
};

function Field({ disabled = false, label, onChange, value }: FieldProps) {
  return (
    <label className="form-control w-full gap-2">
      <span className="label-text font-medium">{label}</span>
      <input
        className="input input-bordered w-full"
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        value={value}
      />
    </label>
  );
}

type InviteSuggestionFieldProps = {
  label: string;
  loading: boolean;
  onChange: (value: string) => void;
  onFocus: () => void;
  onSelectSuggestion: (suggestion: GroupInviteSuggestion) => void;
  open: boolean;
  suggestions: GroupInviteSuggestion[];
  suggestionsError: string | null;
  value: string;
  wrapperRef: RefObject<HTMLDivElement | null>;
};

function InviteSuggestionField({
  label,
  loading,
  onChange,
  onFocus,
  onSelectSuggestion,
  open,
  suggestions,
  suggestionsError,
  value,
  wrapperRef
}: InviteSuggestionFieldProps) {
  const showEmptyState = open && !loading && !suggestionsError && suggestions.length === 0;

  return (
    <div ref={wrapperRef} className="form-control w-full gap-2">
      <label className="w-full">
        <span className="label-text font-medium">{label}</span>
        <input
          className="input input-bordered mt-2 w-full"
          onChange={(event) => onChange(event.target.value)}
          onFocus={onFocus}
          value={value}
        />
      </label>

      {open ? (
        <div className="invite-suggestion-panel">
          {loading ? <p className="helper-text">Lade Vorschlaege...</p> : null}
          {suggestionsError ? <p className="helper-text">{suggestionsError}</p> : null}
          {suggestions.map((suggestion) => (
            <button
              key={suggestion.userId}
              className="invite-suggestion-row"
              onClick={() => onSelectSuggestion(suggestion)}
              type="button"
            >
              <span className="subsection-title normal-case tracking-[0.06em]">{suggestion.nickname}</span>
              <span className="helper-text">{suggestion.email}</span>
            </button>
          ))}
          {showEmptyState ? <p className="helper-text">Keine passenden Benutzer gefunden. Du kannst trotzdem per E-Mail einladen.</p> : null}
        </div>
      ) : null}
    </div>
  );
}
