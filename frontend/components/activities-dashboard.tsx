"use client";

import Link from "next/link";
import type { Route } from "next";
import { useEffect, useState } from "react";
import { authenticatedBackendFetch } from "@/lib/authenticated-backend-client";
import {
  activityStatusBadgeClass,
  formatActivityDateTime,
  formatActivityStatus,
  type ActivityError,
  type ActivityListResponse
} from "@/lib/activity";

const initialData: ActivityListResponse = {
  activities: []
};

export function ActivitiesDashboard() {
  const [data, setData] = useState<ActivityListResponse>(initialData);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void authenticatedBackendFetch("/api/private/activities", { cache: "no-store" })
      .then(async (response) => {
        const body = (await response.json()) as ActivityListResponse & ActivityError;

        if (!response.ok) {
          throw new Error(body.message || "Aktivitäten konnten nicht geladen werden");
        }

        setData(body);
        setError(null);
      })
      .catch((loadError) => {
        setError(loadError instanceof Error ? loadError.message : "Aktivitäten konnten nicht geladen werden");
      });
  }, []);

  if (error) {
    return <div className="alert alert-error">{error}</div>;
  }

  return (
    <div className="space-y-6">
      <section className="brand-card card">
        <div className="card-body gap-5">
          <div className="section-intro">
            <p className="section-title">Aktivitäten</p>
            <h1 className="section-headline sm:text-[2.4rem]">Deine anstehenden Termine</h1>
            <p className="subheadline">
              Hier siehst du alle kommenden Aktivitäten, denen du aktuell zugewiesen bist.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <Link className="btn btn-primary" href={"/groups" as Route}>
              Zu den Gruppen
            </Link>
            <Link className="btn btn-outline btn-primary" href={"/" as Route}>
              Startseite
            </Link>
          </div>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {data.activities.map((activity) => (
          <article key={activity.id} className="soft-panel space-y-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="section-title">{activity.groupName}</p>
                <h2 className="subsection-title text-[0.95rem] normal-case tracking-[0.04em]">
                  {activity.description}
                </h2>
              </div>
              <span className={`badge ${activityStatusBadgeClass(activity.currentUserResponseStatus)} badge-outline`}>
                {formatActivityStatus(activity.currentUserResponseStatus)}
              </span>
            </div>

            <div className="space-y-1">
              <p className="body-copy text-sm">{activity.location}</p>
              <p className="helper-text">{formatActivityDateTime(activity.scheduledAt)}</p>
            </div>

            <div className="flex flex-wrap gap-2 text-xs">
              <span className="badge badge-info badge-outline">{activity.participantCounts.open} offen</span>
              <span className="badge badge-success badge-outline">{activity.participantCounts.accepted} zugesagt</span>
              <span className="badge badge-error badge-outline">{activity.participantCounts.declined} abgesagt</span>
              <span className="badge badge-warning badge-outline">{activity.participantCounts.maybe} vielleicht</span>
            </div>

            <Link className="btn btn-sm btn-primary" href={`/groups/${activity.groupId}/activities/${activity.id}` as Route}>
              Details öffnen
            </Link>
          </article>
        ))}
      </section>

      {!data.activities.length ? (
        <section className="soft-panel">
          <p className="helper-text">Keine anstehenden Aktivitäten vorhanden.</p>
        </section>
      ) : null}
    </div>
  );
}
