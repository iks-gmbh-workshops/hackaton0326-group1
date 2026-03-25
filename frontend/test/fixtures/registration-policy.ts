import type { RegistrationPolicy } from "@/lib/registration";

export const registrationPolicyFixture: RegistrationPolicy = {
  nickname: {
    minLength: 3,
    maxLength: 255
  },
  password: {
    minLength: 8,
    minUpperCase: 1,
    minLowerCase: 1,
    minDigits: 1,
    minSpecialChars: 1
  },
  captcha: {
    mode: "mock",
    mockPassToken: "test-pass"
  },
  terms: {
    currentVersion: "2026-03",
    contentSlug: "drumdibum-agb-2026-03",
    url: "/terms/drumdibum-agb-2026-03"
  }
};
