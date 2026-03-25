import { getServerSession } from "next-auth";
import { redirect } from "next/navigation";
import { GroupDetailView } from "@/components/group-detail";
import { authOptions } from "@/lib/auth";

export const dynamic = "force-dynamic";

type GroupDetailPageProps = {
  params: Promise<{ groupId: string }>;
};

export default async function GroupDetailPage({ params }: GroupDetailPageProps) {
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
      <div className="page-container max-w-7xl">
        <GroupDetailView groupId={groupId} />
      </div>
    </main>
  );
}
