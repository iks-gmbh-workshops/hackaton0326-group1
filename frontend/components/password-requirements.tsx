"use client";

import type { PasswordRequirementView } from "@/lib/registration";

type PasswordRequirementsProps = {
  requirements: PasswordRequirementView[];
};

export function PasswordRequirements({ requirements }: PasswordRequirementsProps) {
  return (
    <div className="soft-panel">
      <p className="section-title">Passwortanforderungen</p>
      <ul className="mt-3 space-y-2 text-sm">
        {requirements.map((requirement) => (
            <li
              key={requirement.code}
              className={`flex items-center gap-3 rounded-xl px-3 py-2 ${
                requirement.satisfied ? "bg-success text-success-content" : "bg-white text-base-content"
              }`}
            >
            <span
              className={`inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold ${
                requirement.satisfied
                  ? "bg-success-content text-success"
                  : "bg-secondary text-primary"
              }`}
            >
              {requirement.satisfied ? "OK" : "!"}
            </span>
            <span className="body-copy text-sm">{requirement.label}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
