export type RegistrationPolicy = {
  password: {
    minLength: number;
    minUpperCase: number;
    minLowerCase: number;
    minDigits: number;
    minSpecialChars: number;
  };
  captcha: {
    mode: "mock" | "turnstile";
    mockPassToken?: string | null;
    turnstileSiteKey?: string | null;
  };
};

export type RegistrationPayload = {
  nickname: string;
  password: string;
  passwordRepeat: string;
  email: string;
  captchaToken: string;
  firstName?: string;
  lastName?: string;
};

export type RegistrationError = {
  code: string;
  message: string;
  field?: string | null;
  suggestedNickname?: string | null;
};

export type PasswordRequirementView = {
  code: string;
  label: string;
  satisfied: boolean;
};

export function evaluatePassword(password: string, policy: RegistrationPolicy["password"]): PasswordRequirementView[] {
  const upperCaseCount = [...password].filter((character) => /[A-Z]/.test(character)).length;
  const lowerCaseCount = [...password].filter((character) => /[a-z]/.test(character)).length;
  const digitCount = [...password].filter((character) => /\d/.test(character)).length;
  const specialCharCount = [...password].filter((character) => /[^A-Za-z0-9\s]/.test(character)).length;

  return [
    {
      code: "MIN_LENGTH",
      label: `Mindestens ${policy.minLength} Zeichen`,
      satisfied: password.length >= policy.minLength
    },
    {
      code: "UPPER_CASE",
      label: `Mindestens ${policy.minUpperCase} Grossbuchstabe`,
      satisfied: upperCaseCount >= policy.minUpperCase
    },
    {
      code: "LOWER_CASE",
      label: `Mindestens ${policy.minLowerCase} Kleinbuchstabe`,
      satisfied: lowerCaseCount >= policy.minLowerCase
    },
    {
      code: "DIGIT",
      label: `Mindestens ${policy.minDigits} Ziffer`,
      satisfied: digitCount >= policy.minDigits
    },
    {
      code: "SPECIAL_CHAR",
      label: `Mindestens ${policy.minSpecialChars} Sonderzeichen`,
      satisfied: specialCharCount >= policy.minSpecialChars
    }
  ];
}
