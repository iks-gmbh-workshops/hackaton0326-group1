"use client";

import { useEffect, useId, useRef, useState } from "react";
import type { RegistrationPolicy } from "@/lib/registration";

type CaptchaFieldProps = {
  captcha: RegistrationPolicy["captcha"];
  disabled?: boolean;
  value: string;
  error?: string;
  onChange: (value: string) => void;
};

type TurnstileWindow = Window & {
  turnstile?: {
    render: (
      container: HTMLElement,
      options: {
        sitekey: string;
        callback: (token: string) => void;
        "expired-callback"?: () => void;
        "error-callback"?: () => void;
      }
    ) => string;
    remove: (widgetId: string) => void;
  };
};

export function CaptchaField({ captcha, disabled = false, value, error, onChange }: CaptchaFieldProps) {
  const turnstileContainerRef = useRef<HTMLDivElement | null>(null);
  const widgetIdRef = useRef<string | null>(null);
  const onChangeRef = useRef(onChange);
  const scriptId = useId();
  const [turnstileError, setTurnstileError] = useState<string | null>(null);

  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  useEffect(() => {
    const siteKey = captcha.turnstileSiteKey;

    if (captcha.mode !== "turnstile" || !siteKey) {
      return undefined;
    }

    let cancelled = false;
    const verifiedSiteKey = siteKey;

    function renderWidget() {
      const currentWindow = window as TurnstileWindow;
      if (!turnstileContainerRef.current || !currentWindow.turnstile || cancelled) {
        return;
      }

      if (widgetIdRef.current) {
        currentWindow.turnstile.remove(widgetIdRef.current);
      }

      widgetIdRef.current = currentWindow.turnstile.render(turnstileContainerRef.current, {
        sitekey: verifiedSiteKey,
        callback: (token) => {
          setTurnstileError(null);
          onChangeRef.current(token);
        },
        "expired-callback": () => onChangeRef.current(""),
        "error-callback": () => {
          setTurnstileError("Captcha konnte nicht geladen werden");
          onChangeRef.current("");
        }
      });
    }

    const existingScript = document.getElementById(scriptId) as HTMLScriptElement | null;
    if (existingScript) {
      renderWidget();
      return () => {
        cancelled = true;
      };
    }

    const script = document.createElement("script");
    script.id = scriptId;
    script.src = "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";
    script.async = true;
    script.defer = true;
    script.onload = () => renderWidget();
    script.onerror = () => setTurnstileError("Captcha konnte nicht geladen werden");
    document.body.appendChild(script);

    return () => {
      cancelled = true;
    };
  }, [captcha.mode, captcha.turnstileSiteKey, scriptId]);

  if (captcha.mode === "mock") {
    return (
      <fieldset className="space-y-2">
        <label className="field-label" htmlFor="captchaToken">
          Captcha
        </label>
        <input
          className={`input input-bordered w-full ${error ? "input-error" : ""}`}
          disabled={disabled}
          id="captchaToken"
          name="captchaToken"
          onChange={(event) => onChange(event.target.value)}
          placeholder="Captcha Testtoken eingeben"
          required
          value={value}
        />
        <p className="helper-text">Lokaler Testmodus: verwende `{captcha.mockPassToken ?? "test-pass"}`.</p>
        {error ? <p className="text-sm text-error">{error}</p> : null}
      </fieldset>
    );
  }

  return (
    <fieldset className="space-y-2">
      <label className="field-label">Captcha</label>
      {captcha.turnstileSiteKey ? <div ref={turnstileContainerRef} /> : null}
      {turnstileError ? <p className="text-sm text-error">{turnstileError}</p> : null}
      {error ? <p className="text-sm text-error">{error}</p> : null}
    </fieldset>
  );
}
