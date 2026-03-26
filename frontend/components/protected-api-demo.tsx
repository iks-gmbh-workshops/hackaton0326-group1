"use client";

import { useState, useTransition } from "react";
import { authenticatedBackendFetch } from "@/lib/authenticated-backend-client";

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
      const response = await authenticatedBackendFetch("/api/private/me", {
        method: "GET",
        cache: "no-store"
      });

      const text = await response.text();

      if (!response.ok) {
        setPayload("");
        setError(text || "Der geschützte Backend-Call ist fehlgeschlagen.");
        return;
      }

      setPayload(text);
    } catch (fetchError) {
      setPayload("");
      setError(
        fetchError instanceof Error
          ? fetchError.message
          : "Der geschützte Backend-Call konnte nicht ausgeführt werden."
      );
    }
  }

  function handleFetch() {
    startTransition(() => {
      void loadPayload();
    });
  }

  return (
    <div className="brand-card card">
      <div className="card-body gap-4">
        <div className="flex items-center justify-between gap-4">
          <div className="section-intro">
            <h2 className="section-headline">Geschützte Benutzerdaten direkt aus dem Spring-Backend laden</h2>
          </div>
          <span className={`badge ${authenticated ? "badge-success" : "badge-neutral"} badge-lg`}>
            {authenticated ? "angemeldet" : "nicht angemeldet"}
          </span>
        </div>

        <p className="subheadline max-w-2xl">
          Der Button lädt die geschützten Benutzerdaten direkt aus dem Backend. Der Bearer-Token ist dabei im Browser-Request sichtbar.
        </p>

        <div className="flex flex-wrap items-center gap-3">
          <button
            className="btn btn-primary"
            disabled={!authenticated || isPending}
            onClick={handleFetch}
          >
            {isPending ? "Lade..." : "Geschützten Call ausführen"}
          </button>
          {!authenticated ? (
            <span className="helper-text">
              Melde Dich zuerst an, um den Endpoint aufzurufen.
            </span>
          ) : null}
        </div>

        <div className="result-surface">
          <pre>{error || payload || "{\n  \"hint\": \"Noch keine Antwort geladen\"\n}"}</pre>
        </div>
      </div>
    </div>
  );
}
