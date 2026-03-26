"use client";

import Link from "next/link";
import type { Route } from "next";
import { usePathname } from "next/navigation";
import { AuthControls } from "@/components/auth-controls";

type AppHeaderProps = {
  authenticated: boolean;
  userName?: string | null;
};

export function AppHeader({ authenticated, userName }: AppHeaderProps) {
  const pathname = usePathname();
  const showPublicLogin = !authenticated && pathname === "/";

  return (
    <header className="sticky top-0 z-30 border-b border-base-300/70 bg-base-100/80 backdrop-blur supports-[backdrop-filter]:bg-base-100/60">
      <div className="mx-auto flex min-h-[4.75rem] w-full max-w-7xl items-center justify-between gap-4 px-3 py-3">
        <Link href="/" className="flex items-center gap-3">
          <span className="brand-kicker">HeuermannPlus</span>
        </Link>

        {authenticated ? (
          <nav className="flex items-center gap-2">
            <Link href={"/groups" as Route} className="btn btn-ghost btn-sm">
              Gruppen
            </Link>

            <button
              type="button"
              className="btn btn-ghost btn-sm"
              disabled
              aria-disabled="true"
              title="Mock: Die Aktivitäten-Route ist noch nicht implementiert."
            >
              Aktivitäten
            </button>

            <button
              type="button"
              className="btn btn-ghost btn-sm h-auto py-2"
              disabled
              aria-disabled="true"
              title="Mock: Die Profil-Route ist noch nicht implementiert."
            >
              <span className="block text-left">
                <span className="block text-[0.7rem] font-semibold uppercase tracking-wider text-base-content/70">
                  Ihr Profil
                </span>
                <span className="block text-[0.9rem] font-semibold">{userName ?? "Nutzer"}</span>
              </span>
            </button>

            <AuthControls authenticated={authenticated} />
          </nav>
        ) : showPublicLogin ? (
          <div className="flex items-center gap-2">
            <AuthControls authenticated={false} variant="header" />
          </div>
        ) : null}
      </div>
    </header>
  );
}
