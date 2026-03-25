import { Suspense } from "react";
import { RegistrationVerifyStatus } from "@/components/registration-verify-status";

export const dynamic = "force-dynamic";

export default function RegisterVerifyPage() {
  return (
    <main className="page-shell">
      <div className="page-container">
        <Suspense fallback={<div className="soft-panel mx-auto max-w-2xl">Lade Verifizierung...</div>}>
          <RegistrationVerifyStatus />
        </Suspense>
      </div>
    </main>
  );
}
