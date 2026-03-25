import { getServerSession } from "next-auth";
import { NextResponse } from "next/server";
import { authOptions } from "@/lib/auth";
import { proxyAuthenticatedBackend } from "@/lib/backend-proxy";

export async function POST(request: Request) {
    const session = await getServerSession(authOptions);

    if (session?.accessToken == null) {
        return NextResponse.json({ error: "Not authenticated" }, { status: 401 });
    }

    const body = await request.text();

    return proxyAuthenticatedBackend(session.accessToken, "/api/private/profile/password", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body
    });
}
