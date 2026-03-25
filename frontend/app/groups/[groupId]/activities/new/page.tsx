import { getServerSession } from "next-auth";
import { redirect } from "next/navigation";
import { ActivityFormView } from "@/components/activity-form-view";
import { authOptions } from "@/lib/auth";

export const dynamic = "force-dynamic";

type NewActivityPageProps = {
  params: Promise<{ groupId: string }>;
};

export default async function NewActivityPage({ params }: NewActivityPageProps) {
  const session = await getServerSession(authOptions);

  if (!session?.user) {
    redirect("/" as never);
  }

  const resolvedParams = await params;
  const groupId = Number(resolvedParams.groupId);

  if (!Number.isFinite(groupId)) {
    redirect("/groups" as never);
  }

  return (
    <main className="page-shell">
      <div className="page-container max-w-6xl">
        <ActivityFormView groupId={groupId} />
      </div>
    </main>
  );
}
