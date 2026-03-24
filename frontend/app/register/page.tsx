import { RegistrationForm } from "@/components/registration-form";

export const dynamic = "force-dynamic";

export default function RegisterPage() {
  return (
    <main className="relative min-h-screen overflow-hidden px-4 py-6 sm:px-6 lg:px-8">
      <div className="ambient-glow ambient-glow-top" />
      <div className="ambient-glow ambient-glow-bottom" />

      <div className="mx-auto w-full max-w-7xl">
        <RegistrationForm />
      </div>
    </main>
  );
}
