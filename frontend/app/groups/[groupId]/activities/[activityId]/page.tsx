import { getServerSession } from "next-auth";
import { redirect } from "next/navigation";
import { ActivityDetailView } from "@/components/activity-detail";
import { authOptions } from "@/lib/auth";

export const dynamic = "force-dynamic";

type ActivityDetailPageProps = {
  params: Promise<{ groupId: string; activityId: string }>;
};

export default async function ActivityDetailPage({ params }: ActivityDetailPageProps) {
  const session = await getServerSession(authOptions);

  if (!session?.user) {
    redirect("/" as never);
  }

  const resolvedParams = await params;
  const groupId = Number(resolvedParams.groupId);
  const activityId = Number(resolvedParams.activityId);

  if (!Number.isFinite(groupId) || !Number.isFinite(activityId)) {
    redirect("/groups" as never);
  }

  return (
    <main className="page-shell">
      <div className="page-container max-w-7xl">
        <ActivityDetailView activityId={activityId} groupId={groupId} />
      </div>
    </main>
  );
}
