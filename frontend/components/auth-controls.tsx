"use client";

import { useTransition } from "react";
import { signIn, signOut } from "next-auth/react";

type AuthControlsProps = {
  authenticated: boolean;
};

export function AuthControls({ authenticated }: AuthControlsProps) {
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
        {isPending ? "Abmelden..." : "Logout"}
      </button>
    );
  }

  return (
    <button className="btn btn-primary" disabled={isPending} onClick={handleLogin}>
      {isPending ? "Weiter..." : "Mit Keycloak anmelden"}
    </button>
  );
}
