"use client";

import type { Route } from "next";
import { getSession, signIn } from "next-auth/react";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState, useTransition } from "react";
import { formatDate, type GroupInvitationResult } from "@/lib/group";

type ResultState =
  | { status: "loading" }
  | { status: "ready"; result: GroupInvitationResult }
  | { status: "error"; message: string };

const invalidResult: GroupInvitationResult = {
  status: "INVALID",
  loginTargetPath: "/"
};

export function GroupInvitationResponse() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [state, setState] = useState<ResultState>({ status: "loading" });
  const [isPending, startTransition] = useTransition();

  useEffect(() => {
    const token = searchParams.get("token");
    const decision = searchParams.get("decision");
    const resolvedDecision = decision === "accept" ? "ACCEPT" : decision === "decline" ? "DECLINE" : null;

    if (!token || resolvedDecision == null) {
      setState({ status: "ready", result: invalidResult });
      return;
    }

    let ignore = false;

    async function respond() {
      const response = await fetch("/api/group-invitations/respond", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          token,
          decision: resolvedDecision
        })
      });

      const body = (await response.json()) as GroupInvitationResult & { message?: string };

      if (ignore) {
        return;
      }

      if (!response.ok) {
        setState({ status: "error", message: body.message ?? "Einladung konnte nicht verarbeitet werden" });
        return;
      }

      setState({ status: "ready", result: body });
    }

    void respond().catch(() => {
      if (!ignore) {
        setState({ status: "error", message: "Einladung konnte nicht verarbeitet werden" });
      }
    });

    return () => {
      ignore = true;
    };
  }, [searchParams]);

  function handleContinue() {
    if (state.status !== "ready") {
      return;
    }

    startTransition(() => {
      void (async () => {
        const targetPath = state.result.loginTargetPath as Route;
        const session = await getSession();
        if (session?.user) {
          router.push(targetPath);
          return;
        }

        await signIn("keycloak", { callbackUrl: state.result.loginTargetPath });
      })();
    });
  }

  if (state.status === "loading") {
    return (
      <div className="soft-panel mx-auto grid w-full max-w-3xl gap-4 rounded-[1.75rem]">
        <h1 className="section-headline">Gruppeneinladung wird verarbeitet</h1>
        <p className="subheadline">Wir verarbeiten gerade deine Antwort aus der E-Mail.</p>
      </div>
    );
  }

  if (state.status === "error") {
    return (
      <div className="soft-panel mx-auto grid w-full max-w-3xl gap-4 rounded-[1.75rem]">
        <h1 className="section-headline">Gruppeneinladung konnte nicht verarbeitet werden</h1>
        <div className="alert alert-error">{state.message}</div>
      </div>
    );
  }

  const { result } = state;

  return (
    <div className="soft-panel mx-auto grid w-full max-w-3xl gap-6 rounded-[1.75rem]">
      <div className="section-intro">
        <h1 className="section-headline">{resultHeading(result)}</h1>
        <p className="subheadline">{resultSubheadline(result)}</p>
      </div>

      {result.status === "ACCEPTED" ? <div className="alert alert-success">Die Einladung wurde angenommen.</div> : null}
      {result.status === "ALREADY_ACCEPTED" ? (
        <div className="alert alert-success">Du hast diese Einladung bereits angenommen.</div>
      ) : null}
      {result.status === "DECLINED" ? <div className="alert alert-info">Die Einladung wurde abgelehnt.</div> : null}
      {result.status === "ALREADY_DECLINED" ? (
        <div className="alert alert-info">Du hast diese Einladung bereits abgelehnt.</div>
      ) : null}
      {result.status === "EXPIRED" ? <div className="alert alert-warning">Der Einladungslink ist abgelaufen.</div> : null}
      {result.status === "INVALID" ? <div className="alert alert-error">Der Einladungslink ist ungueltig.</div> : null}

      <div className="grid gap-4 md:grid-cols-2">
        <div className="rounded-2xl border border-base-300 bg-white/90 p-5">
          <p className="subsection-title">Einladender</p>
          <p className="body-copy mt-2 text-sm">{result.inviterName ?? "Nicht verfuegbar"}</p>
        </div>

        <div className="rounded-2xl border border-base-300 bg-white/90 p-5">
          <p className="subsection-title">Gruppe</p>
          <p className="body-copy mt-2 text-sm">{result.groupName ?? "Nicht verfuegbar"}</p>
        </div>
      </div>

      <div className="rounded-2xl border border-base-300 bg-white/90 p-5">
        <p className="subsection-title">Gruppenbeschreibung</p>
        <p className="body-copy mt-2 text-sm">{result.groupDescription || "Keine Beschreibung hinterlegt."}</p>
      </div>

      <div className="rounded-2xl border border-base-300 bg-white/90 p-5">
        <p className="subsection-title">Naechste Aktivitaet</p>
        {result.nextActivity ? (
          <div className="mt-2 space-y-1 text-sm">
            <p className="body-copy">{result.nextActivity.description}</p>
            <p className="helper-text">{result.nextActivity.location}</p>
            <p className="helper-text">{formatDate(result.nextActivity.scheduledAt)}</p>
          </div>
        ) : (
          <p className="body-copy mt-2 text-sm">Aktuell ist keine anstehende Aktivitaet hinterlegt.</p>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <button className="btn btn-primary" disabled={isPending} onClick={handleContinue} type="button">
          {isPending ? "Weiter..." : continueLabel(result)}
        </button>
      </div>
    </div>
  );
}

function resultHeading(result: GroupInvitationResult) {
  switch (result.status) {
    case "ACCEPTED":
      return "Gruppeneinladung angenommen";
    case "ALREADY_ACCEPTED":
      return "Gruppeneinladung bereits angenommen";
    case "DECLINED":
      return "Gruppeneinladung abgelehnt";
    case "ALREADY_DECLINED":
      return "Gruppeneinladung bereits abgelehnt";
    case "EXPIRED":
      return "Gruppeneinladung abgelaufen";
    case "INVALID":
      return "Gruppeneinladung ungültig";
  }
}

function resultSubheadline(result: GroupInvitationResult) {
  switch (result.status) {
    case "ACCEPTED":
      return "Du kannst dich jetzt anmelden und direkt zur Gruppe weitergehen.";
    case "ALREADY_ACCEPTED":
      return "Du hast diese Einladung schon angenommen und kannst direkt zur Gruppe weitergehen.";
    case "DECLINED":
      return "Die Einladung wurde final abgelehnt.";
    case "ALREADY_DECLINED":
      return "Du hast diese Einladung bereits abgelehnt.";
    case "EXPIRED":
      return "Der Link ist nicht mehr gueltig.";
    case "INVALID":
      return "Bitte pruefe den Link in deiner E-Mail.";
  }
}

function continueLabel(result: GroupInvitationResult) {
  return isAcceptedResult(result) ? "Zur Anmeldung und Gruppe" : "Zur Anmeldung";
}

function isAcceptedResult(result: GroupInvitationResult) {
  return result.status === "ACCEPTED" || result.status === "ALREADY_ACCEPTED";
}
