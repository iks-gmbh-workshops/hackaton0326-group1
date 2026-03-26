import type { Metadata } from "next";
import { Merriweather } from "next/font/google";
import { getServerSession } from "next-auth";
import "./globals.css";
import { AppHeader } from "@/components/app-header";
import { authOptions } from "@/lib/auth";

const headlineFont = Merriweather({
  subsets: ["latin"],
  weight: ["300", "400", "700"],
  style: ["italic"],
  variable: "--font-headline",
  display: "swap"
});

export const metadata: Metadata = {
  title: "HeuermannPlus",
  description: "Scaffold für eine responsive Multilayer-Web-App mit Next.js, Spring Boot und Keycloak.",
  icons: {
    icon: "/icon.svg"
  }
};

export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  const session = await getServerSession(authOptions);
  const authenticated = Boolean(session?.user);
  const displayName = session?.user?.name ?? session?.user?.email ?? null;

  return (
    <html data-theme="light" lang="de">
      <body className={`${headlineFont.variable} app-frame`}>
        <AppHeader authenticated={authenticated} userName={displayName} />
        {children}
      </body>
    </html>
  );
}
