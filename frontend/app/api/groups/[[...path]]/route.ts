import { proxyAuthenticatedBackend } from "@/lib/authenticated-backend-proxy";

async function handle(
  request: Request,
  { params }: { params: Promise<{ path?: string[] }> }
) {
  const resolvedParams = await params;
  const suffix = resolvedParams.path?.length ? `/${resolvedParams.path.join("/")}` : "";
  return proxyAuthenticatedBackend(request, `/api/private/groups${suffix}`);
}

export const GET = handle;
export const HEAD = handle;
export const POST = handle;
export const PATCH = handle;
export const PUT = handle;
export const DELETE = handle;
