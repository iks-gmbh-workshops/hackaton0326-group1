"use client";

import { useTransition } from "react";
import { signIn } from "next-auth/react";

type PublicLoginButtonProps = {
  className?: string;
};

export function PublicLoginButton({ className }: PublicLoginButtonProps) {
  const [isPending, startTransition] = useTransition();

  function handleLogin() {
    startTransition(() => {
      void signIn("keycloak", { callbackUrl: "/" });
    });
  }

  return (
    <button className={className} disabled={isPending} onClick={handleLogin}>
      {isPending ? "Weiter..." : "Anmelden"}
    </button>
  );
}
