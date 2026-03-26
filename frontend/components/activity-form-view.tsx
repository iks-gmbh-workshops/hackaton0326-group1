"use client";

import Link from "next/link";
import type { Route } from "next";
import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { authenticatedBackendFetch } from "@/lib/authenticated-backend-client";
import { combineDateTimeToIso, type ActivityDetail, type ActivityError } from "@/lib/activity";

type ActivityFormViewProps = {
  groupId: number;
};

export function ActivityFormView({ groupId }: ActivityFormViewProps) {
  const router = useRouter();
  const [description, setDescription] = useState("");
  const [details, setDetails] = useState("");
  const [location, setLocation] = useState("");
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [error, setError] = useState<ActivityError | null>(null);
  const [isPending, startTransition] = useTransition();

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    startTransition(() => {
      void authenticatedBackendFetch(`/api/private/groups/${groupId}/activities`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          description,
          details,
          location,
          scheduledAt: date && time ? combineDateTimeToIso(date, time) : ""
        })
      })
        .then(async (response) => {
          const body = (await response.json()) as ActivityDetail & ActivityError;
          if (!response.ok) {
            setError({ code: body.code, message: body.message, field: body.field });
            return;
          }

          router.push(`/groups/${groupId}/activities/${body.id}` as Route);
        })
        .catch(() => {
          setError({ code: "NETWORK_ERROR", message: "Aktivität konnte nicht gespeichert werden" });
        });
    });
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

      <section className="brand-card">
        <form className="grid gap-5 p-6" onSubmit={handleSubmit}>
          <div className="section-intro">
            <p className="section-title">Neue Aktivität</p>
            <h1 className="section-headline sm:text-[2.4rem]">Termin für die Gruppe anlegen</h1>
            <p className="subheadline">
              Alle aktuell aktiven Gruppenmitglieder werden automatisch mit Status offen übernommen.
            </p>
          </div>

          <ActivityField label="Beschreibung" onChange={setDescription} required value={description} />
          <ActivityField label="Ort" onChange={setLocation} required value={location} />
          <div className="grid gap-4 md:grid-cols-2">
            <ActivityField label="Datum" onChange={setDate} required type="date" value={date} />
            <ActivityField label="Uhrzeit" onChange={setTime} required type="time" value={time} />
          </div>
          <ActivityTextarea label="Informationen" onChange={setDetails} value={details} />

          {error ? <div className="alert alert-error">{error.message}</div> : null}

          <button className="form-actions btn btn-primary" disabled={isPending} type="submit">
            {isPending ? "Speichere..." : "Aktivität erstellen"}
          </button>
        </form>
      </section>
    </div>
  );
}

type ActivityFieldProps = {
  label: string;
  onChange: (value: string) => void;
  required?: boolean;
  type?: string;
  value: string;
};

function ActivityField({ label, onChange, required = false, type = "text", value }: ActivityFieldProps) {
  return (
    <label className="form-control w-full gap-2">
      <span className="label-text font-medium">{label}</span>
      <input
        className="input input-bordered w-full"
        onChange={(event) => onChange(event.target.value)}
        required={required}
        type={type}
        value={value}
      />
    </label>
  );
}

type ActivityTextareaProps = {
  label: string;
  onChange: (value: string) => void;
  value: string;
};

function ActivityTextarea({ label, onChange, value }: ActivityTextareaProps) {
  return (
    <label className="form-control w-full gap-2">
      <span className="label-text font-medium">{label}</span>
      <textarea
        className="textarea textarea-bordered min-h-32 w-full"
        onChange={(event) => onChange(event.target.value)}
        value={value}
      />
    </label>
  );
}
