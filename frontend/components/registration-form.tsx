"use client";

import Link from "next/link";
import { useDeferredValue, useEffect, useState, useTransition } from "react";
import { CaptchaField } from "@/components/captcha-field";
import { PasswordRequirements } from "@/components/password-requirements";
import { evaluatePassword, type RegistrationError, type RegistrationPayload, type RegistrationPolicy } from "@/lib/registration";

const initialForm: RegistrationPayload = {
  nickname: "",
  password: "",
  passwordRepeat: "",
  email: "",
  captchaToken: "",
  firstName: "",
  lastName: ""
};

export function RegistrationForm() {
  const [form, setForm] = useState<RegistrationPayload>(initialForm);
  const [policy, setPolicy] = useState<RegistrationPolicy | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<RegistrationError | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();
  const deferredPassword = useDeferredValue(form.password);

  useEffect(() => {
    let ignore = false;

    async function loadPolicy() {
      const response = await fetch("/api/registration/policy", { cache: "no-store" });
      const body = (await response.json()) as RegistrationPolicy | { message?: string };

      if (ignore) {
        return;
      }

      if (!response.ok) {
        setLoadError("Registrierung konnte nicht vorbereitet werden");
        return;
      }

      setPolicy(body as RegistrationPolicy);
    }

    void loadPolicy().catch(() => {
      if (!ignore) {
        setLoadError("Registrierung konnte nicht vorbereitet werden");
      }
    });

    return () => {
      ignore = true;
    };
  }, []);

  const requirements = policy ? evaluatePassword(deferredPassword, policy.password) : [];

  function updateField<K extends keyof RegistrationPayload>(field: K, value: RegistrationPayload[K]) {
    setForm((current) => ({ ...current, [field]: value }));
    setSubmitError((current) => (current?.field === field ? null : current));
  }

  function applySuggestedNickname() {
    if (!submitError?.suggestedNickname) {
      return;
    }

    updateField("nickname", submitError.suggestedNickname);
    setSubmitError(null);
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSuccessMessage(null);

    startTransition(() => {
      void fetch("/api/registration", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(form)
      })
        .then(async (response) => {
          const body = (await response.json()) as { message?: string } & RegistrationError;
          if (!response.ok) {
            setSubmitError({
              code: body.code,
              message: body.message,
              field: body.field,
              suggestedNickname: body.suggestedNickname
            });
            return;
          }

          setSubmitError(null);
          setSuccessMessage(body.message ?? "Die Verifizierungs-E-Mail wurde versendet");
          setForm(initialForm);
        })
        .catch(() => {
          setSubmitError({
            code: "NETWORK_ERROR",
            message: "Registrierung konnte nicht gespeichert werden"
          });
        });
    });
  }

  if (loadError) {
    return <div className="alert alert-error">{loadError}</div>;
  }

  if (!policy) {
    return <div className="alert border-white/10 bg-white/5 text-white">Lade Registrierungsformular...</div>;
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
      <form className="space-y-5 rounded-[2rem] border border-white/10 bg-black/20 p-6 shadow-2xl backdrop-blur" onSubmit={handleSubmit}>
        <div className="space-y-2">
          <p className="text-sm uppercase tracking-[0.25em] text-accent-content/70">Registrierung</p>
          <h1 className="text-3xl font-semibold text-white sm:text-4xl">Erstelle dein drumdibum Konto</h1>
          <p className="text-sm leading-6 text-white/70">
            Nach der Registrierung senden wir dir einen Verifizierungslink per E-Mail. Erst danach wird dein Zugang freigeschaltet.
          </p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            error={submitError?.field === "firstName" ? submitError.message : undefined}
            id="firstName"
            label="Vorname"
            onChange={(value) => updateField("firstName", value)}
            value={form.firstName ?? ""}
          />
          <Field
            error={submitError?.field === "lastName" ? submitError.message : undefined}
            id="lastName"
            label="Name"
            onChange={(value) => updateField("lastName", value)}
            value={form.lastName ?? ""}
          />
        </div>

        <Field
          error={submitError?.field === "nickname" ? submitError.message : undefined}
          id="nickname"
          label="Nickname"
          onChange={(value) => updateField("nickname", value)}
          required
          value={form.nickname}
        />
        {submitError?.field === "nickname" && submitError.suggestedNickname ? (
          <button className="btn btn-sm btn-outline" onClick={applySuggestedNickname} type="button">
            Vorschlag uebernehmen: {submitError.suggestedNickname}
          </button>
        ) : null}

        <Field
          error={submitError?.field === "email" ? submitError.message : undefined}
          id="email"
          label="Email-Adresse"
          onChange={(value) => updateField("email", value)}
          required
          type="email"
          value={form.email}
        />

        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            error={submitError?.field === "password" ? submitError.message : undefined}
            id="password"
            label="Passwort"
            onChange={(value) => updateField("password", value)}
            required
            type="password"
            value={form.password}
          />
          <Field
            error={submitError?.field === "passwordRepeat" ? submitError.message : undefined}
            id="passwordRepeat"
            label="Passwort wiederholen"
            onChange={(value) => updateField("passwordRepeat", value)}
            required
            type="password"
            value={form.passwordRepeat}
          />
        </div>

        <CaptchaField
          captcha={policy.captcha}
          disabled={isPending}
          error={submitError?.field === "captchaToken" ? submitError.message : undefined}
          onChange={(value) => updateField("captchaToken", value)}
          value={form.captchaToken}
        />

        {submitError && !submitError.field ? <div className="alert alert-error">{submitError.message}</div> : null}
        {successMessage ? (
          <div className="alert alert-success">
            <span>{successMessage}</span>
          </div>
        ) : null}

        <div className="flex flex-wrap items-center gap-3">
          <button className="btn btn-accent" disabled={isPending} type="submit">
            {isPending ? "Registriere..." : "Registrieren"}
          </button>
          <Link className="btn btn-ghost text-white hover:bg-white/10" href="/">
            Zur Startseite
          </Link>
        </div>
      </form>

      <div className="space-y-5">
        <PasswordRequirements requirements={requirements} />

        <div className="rounded-[2rem] border border-white/10 bg-white/5 p-5">
          <p className="text-sm uppercase tracking-[0.25em] text-secondary">Ablauf</p>
          <div className="mt-4 space-y-3 text-sm text-white/70">
            <p>1. Formular ausfuellen und Captcha bestaetigen.</p>
            <p>2. Verifizierungs-E-Mail oeffnen und Link klicken.</p>
            <p>3. Nach erfolgreicher Pruefung wird dein Konto freigeschaltet.</p>
          </div>
        </div>

        <div className="rounded-[2rem] border border-white/10 bg-white/5 p-5 text-sm text-white/70">
          <p className="font-medium text-white">Rollenstart</p>
          <p className="mt-2">
            Neue Nutzer starten ohne aktive Anwendungsfreigabe. Erst nach der E-Mail-Verifizierung wird die Rolle
            `app-user` zugewiesen.
          </p>
        </div>
      </div>
    </div>
  );
}

type FieldProps = {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  required?: boolean;
  error?: string;
};

function Field({ id, label, value, onChange, type = "text", required = false, error }: FieldProps) {
  return (
    <fieldset className="space-y-2">
      <label className="text-sm font-medium text-white" htmlFor={id}>
        {label}
      </label>
      <input
        className={`input input-bordered w-full ${error ? "input-error" : ""}`}
        id={id}
        name={id}
        onChange={(event) => onChange(event.target.value)}
        required={required}
        type={type}
        value={value}
      />
      {error ? <p className="text-sm text-error">{error}</p> : null}
    </fieldset>
  );
}
