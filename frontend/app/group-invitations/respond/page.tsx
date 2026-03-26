import { Suspense } from "react";
import { GroupInvitationResponse } from "@/components/group-invitation-response";

export const dynamic = "force-dynamic";

export default function GroupInvitationRespondPage() {
  return (
    <main className="page-shell">
      <div className="page-container">
        <Suspense fallback={<div className="soft-panel mx-auto max-w-3xl">Einladung wird geladen...</div>}>
          <GroupInvitationResponse />
        </Suspense>
      </div>
    </main>
  );
}
