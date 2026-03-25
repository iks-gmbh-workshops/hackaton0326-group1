import { proxyBackend } from "@/lib/backend-proxy";

export async function POST(request: Request) {
  const body = await request.text();

  return proxyBackend("/api/public/registration/verify", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body
  });
}
