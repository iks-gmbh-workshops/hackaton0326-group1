import { Suspense } from "react";
import { RegistrationVerifyStatus } from "@/components/registration-verify-status";

export const dynamic = "force-dynamic";

export default function RegisterVerifyPage() {
  return (
    <main className="relative min-h-screen overflow-hidden px-4 py-6 sm:px-6 lg:px-8">
      <div className="ambient-glow ambient-glow-top" />
      <div className="ambient-glow ambient-glow-bottom" />

      <Suspense fallback={<div className="mx-auto max-w-2xl text-white/70">Lade Verifizierung...</div>}>
        <RegistrationVerifyStatus />
      </Suspense>
    </main>
  );
}
