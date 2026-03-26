"use client";

import Image from "next/image";
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
  const isProfilePage = pathname === "/profile";
  const isRegisterPage = pathname.startsWith("/register");

  const publicNavClass = (href: string) =>
    `btn btn-ghost btn-sm app-nav-link ${pathname === href ? "app-nav-link-active" : ""}`;
  const privateNavClass = (href: string) =>
    `btn btn-ghost btn-sm app-nav-link ${pathname.startsWith(href) ? "app-nav-link-active" : ""}`;

  return (
    <header className="app-header">
      <div className="app-header-inner">
        <Link href="/" className="app-brand">
          <span aria-hidden="true" className="app-brand-mark">
            <Image src="/icon.svg" alt="" width={48} height={48} className="h-full w-full" />
          </span>

          <span className="app-brand-copy">
            <span className="brand-kicker">HeuermannPlus</span>
            <span className="app-brand-title">Gruppen koordinieren, Einladungen steuern, Aktivitäten sichtbar halten</span>
          </span>
        </Link>

        {authenticated ? (
          <>
            <nav className="app-header-nav" aria-label="Hauptnavigation">
              <Link href={"/groups" as Route} className={privateNavClass("/groups")}>
                Gruppen
              </Link>
              <Link href={"/activities" as Route} className={privateNavClass("/activities")}>
                Aktivitäten
              </Link>
            </nav>

            <div className="app-header-actions">
              <Link
                href={"/groups" as Route}
                className={`btn btn-ghost btn-sm app-nav-link lg:hidden ${pathname.startsWith("/groups") ? "app-nav-link-active" : ""}`}
              >
                Gruppen
              </Link>

              <Link
                href={"/activities" as Route}
                className={`btn btn-ghost btn-sm app-nav-link lg:hidden ${pathname.startsWith("/activities") ? "app-nav-link-active" : ""}`}
              >
                Aktivitäten
              </Link>

              <Link
                href={"/profile" as Route}
                aria-current={isProfilePage ? "page" : undefined}
                className={`app-user-chip ${isProfilePage ? "ring-1 ring-[rgba(0,85,120,0.14)]" : ""}`}
              >
                <span className="app-user-label">
                  <span>Profil</span>
                  <strong>{userName ?? "Nutzer"}</strong>
                </span>
                <span className="btn btn-ghost btn-sm">Öffnen</span>
              </Link>

              <AuthControls authenticated={authenticated} />
            </div>
          </>
        ) : (
          <div className="app-header-actions">
            <nav className="app-header-nav" aria-label="Öffentliche Navigation">
              <Link href={"/" as Route} className={publicNavClass("/")}>
                Start
              </Link>
              <Link href={"/register" as Route} className={publicNavClass("/register")}>
                Registrierung
              </Link>
            </nav>

            <Link
              href={"/" as Route}
              className={`btn btn-ghost btn-sm app-nav-link lg:hidden ${pathname === "/" ? "app-nav-link-active" : ""}`}
            >
              Start
            </Link>

            <Link
              href={"/register" as Route}
              className={`btn btn-outline btn-primary btn-sm ${isRegisterPage ? "app-nav-link-active" : ""}`}
            >
              Konto erstellen
            </Link>

            <AuthControls authenticated={false} variant="header" />
          </div>
        )}
      </div>
    </header>
  );
}
