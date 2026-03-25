import { proxyAuthenticatedBackend } from "@/lib/authenticated-backend-proxy";

export async function GET(request: Request) {
  return proxyAuthenticatedBackend(request, "/api/private/me");
}
