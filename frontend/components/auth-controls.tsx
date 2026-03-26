"use client";

import { useTransition } from "react";
import { signIn, signOut } from "next-auth/react";

type AuthControlsProps = {
  authenticated: boolean;
  variant?: "default" | "header";
};

export function AuthControls({ authenticated, variant = "default" }: AuthControlsProps) {
  const [isPending, startTransition] = useTransition();

  function handleLogin() {
    startTransition(() => {
      void signIn("keycloak", { callbackUrl: "/" });
    });
  }

  function handleLogout() {
    startTransition(() => {
      void (async () => {
        const response = await fetch("/api/auth/federated-logout", {
          method: "GET",
          cache: "no-store"
        });

        if (response.ok === false) {
          await signOut({ callbackUrl: "/" });
          return;
        }

        const { logoutUrl } = (await response.json()) as { logoutUrl?: string };

        await signOut({ redirect: false });

        if (logoutUrl) {
          window.location.assign(logoutUrl);
          return;
        }

        window.location.assign("/");
      })();
    });
  }

  if (authenticated) {
    return (
      <button className="btn btn-outline btn-primary" disabled={isPending} onClick={handleLogout}>
        {isPending ? "Abmelden..." : "Abmelden"}
      </button>
    );
  }

  return (
    <button
      className={variant === "header" ? "btn btn-outline btn-primary btn-sm" : "btn btn-primary"}
      disabled={isPending}
      onClick={handleLogin}
    >
      {isPending ? "Weiter..." : variant === "header" ? "Anmelden" : "Mit Keycloak anmelden"}
    </button>
  );
}
