import { getServerSession } from "next-auth";
import { NextResponse } from "next/server";
import { authOptions } from "@/lib/auth";

export async function proxyAuthenticatedBackend(request: Request, path: string) {
  const session = await getServerSession(authOptions);

  if (!session?.accessToken) {
    return NextResponse.json({ error: "Not authenticated" }, { status: 401 });
  }

  const backendUrl = process.env.BACKEND_INTERNAL_URL;

  if (!backendUrl) {
    return NextResponse.json({ error: "BACKEND_INTERNAL_URL is not configured" }, { status: 500 });
  }

  try {
    const requestUrl = new URL(request.url);
    const targetUrl = new URL(path, backendUrl);
    targetUrl.search = requestUrl.search;

    const body =
      request.method === "GET" || request.method === "HEAD"
        ? undefined
        : await request.text();
    const contentType = request.headers.get("content-type");

    const response = await fetch(targetUrl, {
      method: request.method,
      headers: {
        Authorization: `Bearer ${session.accessToken}`,
        Accept: "application/json",
        ...(contentType ? { "Content-Type": contentType } : {})
      },
      body,
      cache: "no-store"
    });

    if (response.status === 204) {
      return new NextResponse(null, { status: 204 });
    }

    const responseContentType = response.headers.get("content-type") ?? "";
    const payload = responseContentType.includes("application/json")
      ? await response.json()
      : { message: await response.text() };

    return NextResponse.json(payload, { status: response.status });
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
