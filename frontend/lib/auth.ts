import type { NextAuthOptions } from "next-auth";
import type { OAuthConfig } from "next-auth/providers/oauth";

type KeycloakProfile = {
  sub: string;
  name?: string;
  email?: string;
  preferred_username?: string;
};

const openIdConnectPath = "/protocol/openid-connect";

function toIssuerUrl(baseUrl: string): string {
  return baseUrl.endsWith(openIdConnectPath)
    ? baseUrl.slice(0, -openIdConnectPath.length)
    : baseUrl;
}

const publicBaseUrl =
  process.env.KEYCLOAK_PUBLIC_BASE_URL ??
  "http://localhost:8081/realms/heuermannplus/protocol/openid-connect";
const internalBaseUrl =
  process.env.KEYCLOAK_INTERNAL_BASE_URL ??
  "http://keycloak:8080/realms/heuermannplus/protocol/openid-connect";
const issuer = process.env.KEYCLOAK_ISSUER ?? toIssuerUrl(publicBaseUrl);
const clientId = process.env.KEYCLOAK_CLIENT_ID ?? "heuermannplus-frontend";
const clientSecret = process.env.KEYCLOAK_CLIENT_SECRET ?? "heuermannplus-frontend-secret";
export const postLogoutRedirectUrl = process.env.NEXTAUTH_URL ?? "http://localhost:3000";
export const keycloakLogoutUrl = `${publicBaseUrl}/logout`;

const keycloakProvider: OAuthConfig<KeycloakProfile> = {
  id: "keycloak",
  name: "Keycloak",
  type: "oauth",
  issuer,
  jwks_endpoint: `${internalBaseUrl}/certs`,
  idToken: true,
  clientId,
  clientSecret,
  checks: ["pkce", "state"],
  authorization: {
    url: `${publicBaseUrl}/auth`,
    params: {
      scope: "openid profile email"
    }
  },
  token: `${internalBaseUrl}/token`,
  userinfo: `${internalBaseUrl}/userinfo`,
  profile(profile) {
    return {
      id: profile.sub,
      name: profile.name ?? profile.preferred_username ?? "HeuermannPlus User",
      email: profile.email
    };
  }
};

export const authOptions: NextAuthOptions = {
  secret: process.env.NEXTAUTH_SECRET ?? "heuermannplus-nextauth-secret-change-me",
  session: {
    strategy: "jwt"
  },
  pages: {
    signIn: "/"
  },
  providers: [keycloakProvider],
  callbacks: {
    async jwt({ token, account }) {
      if (account?.access_token) {
        token.accessToken = account.access_token;
      }

      if (account?.id_token) {
        token.idToken = account.id_token;
      }

      return token;
    },
    async session({ session, token }) {
      session.accessToken = typeof token.accessToken === "string" ? token.accessToken : undefined;
      session.idToken = typeof token.idToken === "string" ? token.idToken : undefined;

      if (session.user) {
        session.user.id = typeof token.sub === "string" ? token.sub : undefined;
      }

      return session;
    }
  }
};
