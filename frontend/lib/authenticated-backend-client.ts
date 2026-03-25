"use client";

import { getSession } from "next-auth/react";

function getPublicBackendUrl() {
  const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080';

  if (!backendUrl) {
    throw new Error("NEXT_PUBLIC_BACKEND_URL ist nicht konfiguriert");
  }

  return backendUrl;
}

export async function authenticatedBackendFetch(path: string, init?: RequestInit) {
  const session = await getSession();

  if (!session?.accessToken) {
    throw new Error("Nicht authentifiziert");
  }

  const targetUrl = new URL(path, getPublicBackendUrl());
  const headers = new Headers(init?.headers);

  headers.set("Authorization", `Bearer ${session.accessToken}`);

  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }

  return fetch(targetUrl, {
    ...init,
    headers,
    cache: "no-store"
  });
}
