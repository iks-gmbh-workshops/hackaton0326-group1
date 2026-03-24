import type { NextAuthOptions } from "next-auth";
import type { OAuthConfig } from "next-auth/providers/oauth";

type KeycloakProfile = {
  sub: string;
  name?: string;
  email?: string;
  preferred_username?: string;
};

const publicBaseUrl =
  process.env.KEYCLOAK_PUBLIC_BASE_URL ??
  "http://localhost:8081/realms/heuermannplus/protocol/openid-connect";
const internalBaseUrl =
  process.env.KEYCLOAK_INTERNAL_BASE_URL ??
  "http://keycloak:8080/realms/heuermannplus/protocol/openid-connect";
const clientId = process.env.KEYCLOAK_CLIENT_ID ?? "heuermannplus-frontend";
const clientSecret = process.env.KEYCLOAK_CLIENT_SECRET ?? "heuermannplus-frontend-secret";

const keycloakProvider: OAuthConfig<KeycloakProfile> = {
  id: "keycloak",
  name: "Keycloak",
  type: "oauth",
  idToken: false,
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

      return token;
    },
    async session({ session, token }) {
      session.accessToken = typeof token.accessToken === "string" ? token.accessToken : undefined;

      if (session.user) {
        session.user.id = typeof token.sub === "string" ? token.sub : undefined;
      }

      return session;
    }
  }
};
