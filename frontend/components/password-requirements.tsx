"use client";

import type { PasswordRequirementView } from "@/lib/registration";

type PasswordRequirementsProps = {
  requirements: PasswordRequirementView[];
};

export function PasswordRequirements({ requirements }: PasswordRequirementsProps) {
  return (
    <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
      <p className="text-sm font-medium text-white">Passwortanforderungen</p>
      <ul className="mt-3 space-y-2 text-sm">
        {requirements.map((requirement) => (
          <li
            key={requirement.code}
            className={`flex items-center gap-3 rounded-xl px-3 py-2 ${
              requirement.satisfied ? "bg-success/15 text-success-content" : "bg-white/5 text-white/70"
            }`}
          >
            <span
              className={`inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold ${
                requirement.satisfied ? "bg-success text-success-content" : "bg-white/10 text-white"
              }`}
            >
              {requirement.satisfied ? "OK" : "!"}
            </span>
            <span>{requirement.label}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
