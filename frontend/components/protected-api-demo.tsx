"use client";

import { useState, useTransition } from "react";

type ProtectedApiDemoProps = {
  authenticated: boolean;
};

export function ProtectedApiDemo({ authenticated }: ProtectedApiDemoProps) {
  const [payload, setPayload] = useState<string>("");
  const [error, setError] = useState<string>("");
  const [isPending, startTransition] = useTransition();

  async function loadPayload() {
    setError("");

    try {
      const response = await fetch("/api/me", {
        method: "GET",
        cache: "no-store"
      });

      const text = await response.text();

      if (!response.ok) {
        setPayload("");
        setError(text || "Der geschuetzte Backend-Call ist fehlgeschlagen.");
        return;
      }

      setPayload(text);
    } catch (fetchError) {
      setPayload("");
      setError(
        fetchError instanceof Error
          ? fetchError.message
          : "Der geschuetzte Backend-Call konnte nicht ausgefuehrt werden."
      );
    }
  }

  function handleFetch() {
    startTransition(() => {
      void loadPayload();
    });
  }

  return (
    <div className="card border border-base-200 bg-base-100 shadow-xl">
      <div className="card-body gap-4">
        <div className="flex items-center justify-between gap-4">
          <div>
            <p className="text-sm uppercase tracking-[0.25em] text-accent">Protected API Demo</p>
            <h2 className="text-2xl font-semibold text-base-content">BFF ruft das Spring-Backend auf</h2>
          </div>
          <span className={`badge ${authenticated ? "badge-success" : "badge-ghost"} badge-lg`}>
            {authenticated ? "angemeldet" : "nicht angemeldet"}
          </span>
        </div>

        <p className="max-w-2xl text-sm leading-6 text-base-content/70">
          Der Button laedt ueber den Next.js-Route-Handler `/api/me` die geschuetzten Benutzerdaten aus dem Backend.
        </p>

        <div className="flex flex-wrap items-center gap-3">
          <button
            className="btn btn-secondary"
            disabled={!authenticated || isPending}
            onClick={handleFetch}
          >
            {isPending ? "Lade..." : "Geschuetzten Call ausfuehren"}
          </button>
          {!authenticated ? (
            <span className="text-sm text-base-content/60">
              Melde Dich zuerst an, um den Endpoint aufzurufen.
            </span>
          ) : null}
        </div>

        <div className="min-h-44 rounded-2xl border border-base-200 bg-neutral text-neutral-content">
          <pre className="h-full overflow-x-auto p-4 text-sm leading-6">
            {error || payload || "{\n  \"hint\": \"Noch keine Antwort geladen\"\n}"}
          </pre>
        </div>
      </div>
    </div>
  );
}
