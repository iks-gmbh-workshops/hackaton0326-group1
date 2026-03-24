import { proxyBackend } from "@/lib/backend-proxy";

export async function GET() {
  return proxyBackend("/api/public/registration/policy", { method: "GET" });
}
