"use client";

import type { Route } from "next";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import type { RegistrationError } from "@/lib/registration";

type VerificationState =
  | { status: "loading" }
  | { status: "success"; message: string }
  | { status: "error"; message: string };

export function RegistrationVerifyStatus() {
  const searchParams = useSearchParams();
  const [state, setState] = useState<VerificationState>({ status: "loading" });

  useEffect(() => {
    const token = searchParams.get("token");

    if (!token) {
      setState({ status: "error", message: "Der Verifizierungslink ist ungültig" });
      return;
    }

    let ignore = false;

    async function verify() {
      const response = await fetch("/api/registration/verify", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ token })
      });

      const body = (await response.json()) as { message?: string } & RegistrationError;

      if (ignore) {
        return;
      }

      if (!response.ok) {
        setState({ status: "error", message: body.message ?? "Verifizierung fehlgeschlagen" });
        return;
      }

      setState({ status: "success", message: body.message ?? "Die Registrierung wurde erfolgreich bestätigt" });
    }

    void verify().catch(() => {
      if (!ignore) {
        setState({ status: "error", message: "Verifizierung fehlgeschlagen" });
      }
    });

    return () => {
      ignore = true;
    };
  }, [searchParams]);

  return (
    <section className="public-verification-shell">
      <div className="public-callout public-verification-card">
        <div className="section-intro">
          <h1 className="display-headline">Deinen Zugang bestätigen und die Registrierung abschließen.</h1>
          <p className="body-copy">
            Die Rückmeldung unten kommt direkt aus dem bestehenden Verifizierungsprozess. Das Redesign ändert nicht den
            Ablauf, sondern macht Erfolg und Fehler eindeutiger sichtbar.
          </p>
        </div>

        {state.status === "loading" ? <div className="alert alert-info">Wir prüfen deinen Verifizierungslink...</div> : null}
        {state.status === "success" ? <div className="alert alert-success">{state.message}</div> : null}
        {state.status === "error" ? <div className="alert alert-error">{state.message}</div> : null}

        <div className="public-stage-list">
          <div className="public-stage-row">
            <span className="public-stage-number">1</span>
            <p className="body-copy text-sm">Der Token wird gegen den bestehenden Backend-Prozess validiert.</p>
          </div>
          <div className="public-stage-row">
            <span className="public-stage-number">2</span>
            <p className="body-copy text-sm">Bei Erfolg ist die Registrierung abgeschlossen und dein Zugang kann genutzt werden.</p>
          </div>
        </div>

        <div className="flex flex-wrap gap-3">
          <Link className="btn btn-primary" href={"/" as Route}>
            Zur Anmeldung
          </Link>
          <Link className="btn btn-ghost" href={"/register" as Route}>
            Neue Registrierung
          </Link>
        </div>
      </div>
    </section>
  );
}
