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
    <div className="mx-auto w-full max-w-2xl rounded-[2rem] border border-white/10 bg-black/20 p-6 shadow-2xl backdrop-blur">
      <p className="text-sm uppercase tracking-[0.25em] text-secondary">Verifizierung</p>
      <h1 className="mt-3 text-3xl font-semibold text-white">Registrierung bestaetigen</h1>

      {state.status === "loading" ? (
        <p className="mt-4 text-white/70">Wir pruefen deinen Verifizierungslink...</p>
      ) : null}

      {state.status === "success" ? <div className="alert alert-success mt-5">{state.message}</div> : null}
      {state.status === "error" ? <div className="alert alert-error mt-5">{state.message}</div> : null}

      <div className="mt-6 flex flex-wrap gap-3">
        <Link className="btn btn-accent" href="/">
          Zur Anmeldung
        </Link>
        <Link className="btn btn-ghost text-white hover:bg-white/10" href="/register">
          Neue Registrierung
        </Link>
      </div>
    </div>
  );
}
