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
  const isProfilePage = pathname === "/profile";

  return (
    <header className="sticky top-0 z-30 border-b border-base-300/80 bg-base-100/94">
      <div className="mx-auto flex min-h-[4.75rem] w-full max-w-7xl items-center justify-between gap-4 px-3 py-3">
        <Link href="/" className="flex items-center gap-3">
          <span
            aria-hidden="true"
            className="flex h-10 w-10 items-center justify-center rounded-xl border border-[rgba(0,85,120,0.14)] bg-[#f3f6f7] shadow-sm"
          >
            <svg viewBox="0 0 256 256" className="h-7 w-7" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path
                d="M60 52C60 47.5817 63.5817 44 68 44H88C92.4183 44 96 47.5817 96 52V112H160V52C160 47.5817 163.582 44 168 44H188C192.418 44 196 47.5817 196 52V204C196 208.418 192.418 212 188 212H168C163.582 212 160 208.418 160 204V144H96V204C96 208.418 92.4183 212 88 212H68C63.5817 212 60 208.418 60 204V52Z"
                fill="#005578"
              />
              <path
                d="M173 70C173 67.2386 175.239 65 178 65H190V53C190 50.2386 192.239 48 195 48H205C207.761 48 210 50.2386 210 53V65H222C224.761 65 227 67.2386 227 70V80C227 82.7614 224.761 85 222 85H210V97C210 99.7614 207.761 102 205 102H195C192.239 102 190 99.7614 190 97V85H178C175.239 85 173 82.7614 173 80V70Z"
                fill="#8AA6B0"
              />
              <path d="M128 96L160 128L128 160L96 128L128 96Z" fill="#D7E4E8" fillOpacity="0.9" />
            </svg>
          </span>
          <span className="brand-kicker">HeuermannPlus</span>
        </Link>

        {authenticated ? (
          <nav className="flex flex-wrap items-center justify-end gap-2">
            <Link href={"/groups" as Route} className="btn btn-ghost btn-sm">
              Gruppen
            </Link>

            <Link href={"/activities" as Route} className="btn btn-ghost btn-sm">
              Aktivitäten
            </Link>

            <Link
              href={"/profile" as Route}
              aria-current={isProfilePage ? "page" : undefined}
              className={`btn btn-ghost btn-sm h-auto py-2 ${isProfilePage ? "bg-base-200" : ""}`}
            >
              <span className="block text-left">
                <span className="block text-[0.7rem] font-semibold uppercase tracking-wider text-base-content/70">
                  Ihr Profil
                </span>
                <span className="block text-[0.9rem] font-semibold">{userName ?? "Nutzer"}</span>
              </span>
            </Link>

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
