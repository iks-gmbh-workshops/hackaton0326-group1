import { getServerSession } from "next-auth";
import { redirect } from "next/navigation";
import { ProfileManagement } from "@/components/profile-management";
import { authOptions } from "@/lib/auth";

export const dynamic = "force-dynamic";

export default async function ProfilePage() {
    const session = await getServerSession(authOptions);

    if (session?.user == null) {
        redirect("/");
    }

    return <ProfileManagement />;
}
