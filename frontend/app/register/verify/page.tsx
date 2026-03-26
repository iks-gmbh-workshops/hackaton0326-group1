import { Suspense } from "react";
import { RegistrationVerifyStatus } from "@/components/registration-verify-status";

export const dynamic = "force-dynamic";

export default function RegisterVerifyPage() {
  return (
    <main className="page-shell">
      <div className="page-container public-shell">
        <Suspense fallback={<div className="soft-panel public-loading-panel">Lade Verifizierung...</div>}>
          <RegistrationVerifyStatus />
        </Suspense>
      </div>
    </main>
  );
}
