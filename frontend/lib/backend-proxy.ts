import { NextResponse } from "next/server";

export async function proxyBackend(path: string, init?: RequestInit) {
  const backendUrl = process.env.BACKEND_INTERNAL_URL;

  if (!backendUrl) {
    return NextResponse.json({ error: "BACKEND_INTERNAL_URL is not configured" }, { status: 500 });
  }

  try {
    const response = await fetch(`${backendUrl}${path}`, {
      ...init,
      headers: {
        Accept: "application/json",
        ...(init?.headers ?? {})
      },
      cache: "no-store"
    });

    const contentType = response.headers.get("content-type") ?? "";
    const isJson = contentType.includes("application/json");
    const body = isJson ? await response.json() : { message: await response.text() };

    return NextResponse.json(body, { status: response.status });
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
