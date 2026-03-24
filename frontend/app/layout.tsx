import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "HeuermannPlus",
  description: "Scaffold fuer eine responsive Multilayer-Web-App mit Next.js, Spring Boot und Keycloak."
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html data-theme="forest" lang="de">
      <body>{children}</body>
    </html>
  );
}
