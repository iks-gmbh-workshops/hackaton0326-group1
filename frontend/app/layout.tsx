import type { Metadata } from "next";
import { Merriweather } from "next/font/google";
import "./globals.css";

const headlineFont = Merriweather({
  subsets: ["latin"],
  weight: ["300", "400", "700"],
  style: ["italic"],
  variable: "--font-headline",
  display: "swap"
});

export const metadata: Metadata = {
  title: "HeuermannPlus",
  description: "HeuermannPlus unterstuetzt Gruppen, Mitglieder und gemeinsame Aktivitaeten in einer klaren Weboberflaeche."
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html data-theme="light" lang="de">
      <body className={headlineFont.variable}>{children}</body>
    </html>
  );
}
