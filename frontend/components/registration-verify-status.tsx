"use client";

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
      setState({ status: "error", message: "Der Verifizierungslink ist ungueltig" });
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

      setState({ status: "success", message: body.message ?? "Die Registrierung wurde erfolgreich bestaetigt" });
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
    <div className="soft-panel mx-auto grid w-full max-w-2xl gap-4 rounded-[1.75rem]">
      <p className="section-title">Verifizierung</p>
      <h1 className="section-headline">Registrierung bestaetigen</h1>

      {state.status === "loading" ? (
        <p className="subheadline">Wir pruefen deinen Verifizierungslink...</p>
      ) : null}

      {state.status === "success" ? <div className="alert alert-success">{state.message}</div> : null}
      {state.status === "error" ? <div className="alert alert-error">{state.message}</div> : null}

      <div className="flex flex-wrap gap-3">
        <Link className="btn btn-primary" href="/">
          Zur Anmeldung
        </Link>
        <Link className="btn btn-ghost" href="/register">
          Neue Registrierung
        </Link>
      </div>
    </div>
  );
}
