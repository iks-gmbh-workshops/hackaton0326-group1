import { getServerSession } from "next-auth";
import { AuthControls } from "@/components/auth-controls";
import { ProtectedApiDemo } from "@/components/protected-api-demo";
import { authOptions } from "@/lib/auth";

export const dynamic = "force-dynamic";

const highlights = [
  "Mobile-first Layout mit Next.js App Router",
  "DaisyUI-Komponenten auf Tailwind CSS 4",
  "Keycloak Login ueber serverseitige Session",
  "Spring Boot Resource Server mit JWT-Validierung"
];

const steps = [
  "Frontend startet den Login ueber Keycloak.",
  "next-auth speichert die Session im Frontend.",
  "Der Route-Handler `/api/me` fungiert als BFF.",
  "Das Backend validiert den Bearer-Token gegen Keycloak."
];

export default async function HomePage() {
  const session = await getServerSession(authOptions);
  const authenticated = Boolean(session?.user);

  return (
    <main className="relative min-h-screen overflow-hidden px-4 py-6 sm:px-6 lg:px-8">
      <div className="ambient-glow ambient-glow-top" />
      <div className="ambient-glow ambient-glow-bottom" />

      <div className="mx-auto flex w-full max-w-7xl flex-col gap-6">
        <section className="hero-panel">
          <div className="space-y-6">
            <div className="inline-flex items-center gap-3 rounded-full border border-white/15 bg-white/8 px-4 py-2 text-xs uppercase tracking-[0.3em] text-accent-content/80">
              HeuermannPlus
              <span className="badge badge-accent badge-sm">Scaffold</span>
            </div>

            <div className="max-w-3xl space-y-4">
              <h1 className="text-balance text-4xl font-semibold tracking-tight text-white sm:text-5xl lg:text-6xl">
                Multilayer-Web-App mit klarer Trennung zwischen UI, API und IAM.
              </h1>
              <p className="max-w-2xl text-base leading-7 text-white/75 sm:text-lg">
                Dieses Grundgeruest verbindet ein responsives Next.js-Frontend, ein Kotlin/Spring-Boot-Backend
                und Keycloak als Identity-Layer. Die lokale Entwicklung startet komplett ueber Docker Compose.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <AuthControls authenticated={authenticated} />
              <a className="btn btn-ghost text-white hover:bg-white/10" href="#architecture">
                Architektur ansehen
              </a>
            </div>
          </div>

          <div className="grid gap-4 rounded-[2rem] border border-white/10 bg-black/20 p-5 shadow-2xl backdrop-blur">
            <div className="flex items-center justify-between">
              <p className="text-sm uppercase tracking-[0.25em] text-white/60">Session Status</p>
              <span className={`badge ${authenticated ? "badge-success" : "badge-ghost"} badge-lg`}>
                {authenticated ? "aktiv" : "inaktiv"}
              </span>
            </div>

            <div className="space-y-2">
              <h2 className="text-2xl font-semibold text-white">
                {authenticated ? `Willkommen ${session?.user?.name ?? "zurueck"}` : "Bereit fuer den ersten Login"}
              </h2>
              <p className="text-sm leading-6 text-white/65">
                {authenticated
                  ? "Die Session liegt serverseitig vor und kann jetzt ueber den BFF sicher an das Backend weitergegeben werden."
                  : "Starte den Login ueber Keycloak, um den geschuetzten Backend-Endpoint live aus dem Frontend aufzurufen."}
              </p>
            </div>

            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/80">
              <p className="font-medium text-white">Aktueller Benutzer</p>
              <p>{session?.user?.email ?? "Nicht angemeldet"}</p>
            </div>
          </div>
        </section>

        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {highlights.map((highlight) => (
            <article
              key={highlight}
              className="rounded-[1.75rem] border border-base-200 bg-base-100/90 p-5 shadow-lg backdrop-blur"
            >
              <p className="text-sm leading-6 text-base-content/75">{highlight}</p>
            </article>
          ))}
        </section>

        <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]" id="architecture">
          <ProtectedApiDemo authenticated={authenticated} />

          <div className="card border border-base-200 bg-base-100 shadow-xl">
            <div className="card-body gap-5">
              <div>
                <p className="text-sm uppercase tracking-[0.25em] text-secondary">Flow</p>
                <h2 className="text-2xl font-semibold">Wie die Demo zusammenspielt</h2>
              </div>

              <div className="space-y-3">
                {steps.map((step, index) => (
                  <div key={step} className="flex gap-4 rounded-2xl border border-base-200 bg-base-200/40 p-4">
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-secondary font-semibold text-secondary-content">
                      {index + 1}
                    </span>
                    <p className="text-sm leading-6 text-base-content/75">{step}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
