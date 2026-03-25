"use client";

import Link from "next/link";
import { useDeferredValue, useEffect, useState, useTransition } from "react";
import { CaptchaField } from "@/components/captcha-field";
import { PasswordRequirements } from "@/components/password-requirements";
import { evaluatePassword, type RegistrationError, type RegistrationPayload, type RegistrationPolicy, validateNickname } from "@/lib/registration";

const initialForm: RegistrationPayload = {
  nickname: "",
  password: "",
  passwordRepeat: "",
  email: "",
  captchaToken: "",
  firstName: "",
  lastName: "",
  acceptTerms: false
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
  const nicknameValidationError = policy ? validateNickname(form.nickname, policy.nickname) : null;

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

    if (policy) {
      const nicknameError = validateNickname(form.nickname, policy.nickname);
      if (nicknameError) {
        setSubmitError({
          code: "INVALID_NICKNAME",
          message: nicknameError,
          field: "nickname"
        });
        return;
      }

      if (!form.acceptTerms) {
        setSubmitError({
          code: "TERMS_NOT_ACCEPTED",
          message: "Bitte die Nutzungsbedingungen akzeptieren",
          field: "acceptTerms"
        });
        return;
      }
    }

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
    return <div className="soft-panel">Lade Registrierungsformular...</div>;
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
      <form className="brand-card space-y-5 p-6" onSubmit={handleSubmit}>
        <div className="space-y-2">
          <p className="section-title">Registrierung</p>
          <h1 className="section-headline">Erstelle dein drumdibum Konto</h1>
          <p className="subheadline">
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
          error={submitError?.field === "nickname" ? submitError.message : nicknameValidationError ?? undefined}
          id="nickname"
          label="Nickname"
          maxLength={policy.nickname.maxLength}
          minLength={policy.nickname.minLength}
          onChange={(value) => updateField("nickname", value)}
          required
          value={form.nickname}
        />
        {!submitError?.field || submitError.field !== "nickname" ? (
          <p className="helper-text">
            Zwischen {policy.nickname.minLength} und {policy.nickname.maxLength} Zeichen.
          </p>
        ) : null}
        {submitError?.field === "nickname" && submitError.suggestedNickname ? (
          <button className="btn btn-sm btn-outline btn-primary" onClick={applySuggestedNickname} type="button">
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

        <fieldset className="space-y-2">
          <label className="terms-checkbox-row" htmlFor="acceptTerms">
            <input
              checked={form.acceptTerms}
              className={`checkbox checkbox-sm ${submitError?.field === "acceptTerms" ? "checkbox-error" : "checkbox-primary"}`}
              id="acceptTerms"
              name="acceptTerms"
              onChange={(event) => updateField("acceptTerms", event.target.checked)}
              required
              type="checkbox"
            />
            <span className="body-copy text-sm leading-7">
              Ich stimme den{" "}
              <a
                className="text-primary underline decoration-[0.08em] underline-offset-3"
                href={policy.terms.url}
                rel="noreferrer"
                target="_blank"
              >
                Nutzungsbedingungen
              </a>{" "}
              in Version {policy.terms.currentVersion} zu.
            </span>
          </label>
          {submitError?.field === "acceptTerms" ? <p className="text-sm text-error">{submitError.message}</p> : null}
          <p className="helper-text">Die Registrierung ist nur moeglich, wenn du der aktuell gueltigen Version zustimmst.</p>
        </fieldset>

        {submitError && !submitError.field ? <div className="alert alert-error">{submitError.message}</div> : null}
        {successMessage ? (
          <div className="alert alert-success">
            <span>{successMessage}</span>
          </div>
        ) : null}

        <div className="flex flex-wrap items-center gap-3">
          <button className="btn btn-primary" disabled={isPending} type="submit">
            {isPending ? "Registriere..." : "Registrieren"}
          </button>
          <Link className="btn btn-ghost" href="/">
            Zur Startseite
          </Link>
        </div>
      </form>

      <div className="space-y-5">
        <PasswordRequirements requirements={requirements} />

        <div className="soft-panel">
          <p className="section-title">Ablauf</p>
          <div className="mt-4 space-y-3">
            <p className="body-copy text-sm">1. Formular ausfuellen, Captcha bestaetigen und der aktuellen AGB-Version zustimmen.</p>
            <p className="body-copy text-sm">2. Verifizierungs-E-Mail oeffnen und Link klicken.</p>
            <p className="body-copy text-sm">3. Nach erfolgreicher Pruefung wird dein Konto freigeschaltet.</p>
          </div>
        </div>

        <div className="soft-panel text-sm">
          <p className="subsection-title">Rollenstart</p>
          <p className="body-copy mt-2 text-sm">
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
  minLength?: number;
  maxLength?: number;
};

function Field({ id, label, value, onChange, type = "text", required = false, error, minLength, maxLength }: FieldProps) {
  return (
    <fieldset className="space-y-2">
      <label className="field-label" htmlFor={id}>
        {label}
      </label>
      <input
        className={`input input-bordered w-full ${error ? "input-error" : ""}`}
        id={id}
        maxLength={maxLength}
        minLength={minLength}
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
