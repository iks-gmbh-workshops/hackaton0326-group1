import { NextRequest, NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";
import { keycloakLogoutUrl, postLogoutRedirectUrl } from "@/lib/auth";

export async function GET(request: NextRequest) {
  const token = await getToken({
    req: request,
    secret: process.env.NEXTAUTH_SECRET ?? "heuermannplus-nextauth-secret-change-me"
  });

  const logoutUrl = new URL(keycloakLogoutUrl);
  logoutUrl.searchParams.set("post_logout_redirect_uri", postLogoutRedirectUrl);

  if (typeof token?.idToken === "string" && token.idToken.length > 0) {
    logoutUrl.searchParams.set("id_token_hint", token.idToken);
  }

  return NextResponse.json({ logoutUrl: logoutUrl.toString() });
}
