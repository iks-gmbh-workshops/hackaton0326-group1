import { getServerSession } from "next-auth";
import { NextResponse } from "next/server";
import { authOptions } from "@/lib/auth";
import { proxyAuthenticatedBackend } from "@/lib/backend-proxy";

export async function GET() {
    const session = await getServerSession(authOptions);

    if (session?.accessToken == null) {
        return NextResponse.json({ error: "Not authenticated" }, { status: 401 });
    }

    return proxyAuthenticatedBackend(session.accessToken, "/api/private/profile", {
        method: "GET"
    });
}

export async function PUT(request: Request) {
    const session = await getServerSession(authOptions);

    if (session?.accessToken == null) {
        return NextResponse.json({ error: "Not authenticated" }, { status: 401 });
    }

    const body = await request.text();

    return proxyAuthenticatedBackend(session.accessToken, "/api/private/profile", {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body
    });
}

export async function DELETE(request: Request) {
    const session = await getServerSession(authOptions);

    if (session?.accessToken == null) {
        return NextResponse.json({ error: "Not authenticated" }, { status: 401 });
    }

    const body = await request.text();

    return proxyAuthenticatedBackend(session.accessToken, "/api/private/profile", {
        method: "DELETE",
        headers: {
            "Content-Type": "application/json"
        },
        body
    });
}
