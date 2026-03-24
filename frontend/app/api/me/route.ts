import { getServerSession } from "next-auth";
import { NextResponse } from "next/server";
import { authOptions } from "@/lib/auth";

export async function GET() {
  const session = await getServerSession(authOptions);

  if (!session?.accessToken) {
    return NextResponse.json({ error: "Not authenticated" }, { status: 401 });
  }

  const backendUrl = process.env.BACKEND_INTERNAL_URL;

  if (!backendUrl) {
    return NextResponse.json({ error: "BACKEND_INTERNAL_URL is not configured" }, { status: 500 });
  }

  try {
    const response = await fetch(`${backendUrl}/api/private/me`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
        Accept: "application/json"
      },
      cache: "no-store"
    });

    const contentType = response.headers.get("content-type");
    const isJson = contentType?.includes("application/json");
    const body = isJson ? await response.json() : await response.text();

    if (!response.ok) {
      return NextResponse.json(
        {
          error: "Backend request failed",
          details: body
        },
        { status: response.status }
      );
    }

    return NextResponse.json(body);
  } catch (error) {
    return NextResponse.json(
      {
        error: "Backend request failed",
        details: error instanceof Error ? error.message : "Unknown error"
      },
      { status: 502 }
    );
  }
}
