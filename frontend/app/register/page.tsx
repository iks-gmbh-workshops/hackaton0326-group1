import { RegistrationForm } from "@/components/registration-form";

export const dynamic = "force-dynamic";

export default function RegisterPage() {
  return (
    <main className="page-shell">
      <div className="page-container max-w-7xl">
        <RegistrationForm />
      </div>
    </main>
  );
}
