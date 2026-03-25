import { getServerSession } from "next-auth";
import { redirect } from "next/navigation";
import { GroupsDashboard } from "@/components/groups-dashboard";
import { authOptions } from "@/lib/auth";

export const dynamic = "force-dynamic";

export default async function GroupsPage() {
  const session = await getServerSession(authOptions);

  if (!session?.user) {
    redirect("/");
  }

  return (
    <main className="page-shell">
      <div className="page-container max-w-7xl">
        <GroupsDashboard />
      </div>
    </main>
  );
}
