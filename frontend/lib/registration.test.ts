import { describe, expect, it } from "vitest";
import { evaluatePassword, validateNickname } from "@/lib/registration";
import { registrationPolicyFixture } from "@/test/fixtures/registration-policy";

describe("registration helpers", () => {
  it("validates nickname boundaries", () => {
    expect(validateNickname(" ab ", registrationPolicyFixture.nickname)).toBe(
      "Nickname muss zwischen 3 und 255 Zeichen lang sein"
    );
    expect(validateNickname(" drummer ", registrationPolicyFixture.nickname)).toBeNull();
  });

  it("evaluates password requirements individually", () => {
    const requirements = evaluatePassword("Weakpass", registrationPolicyFixture.password);

    expect(requirements).toEqual([
      { code: "MIN_LENGTH", label: "Mindestens 8 Zeichen", satisfied: true },
      { code: "UPPER_CASE", label: "Mindestens 1 Grossbuchstabe", satisfied: true },
      { code: "LOWER_CASE", label: "Mindestens 1 Kleinbuchstabe", satisfied: true },
      { code: "DIGIT", label: "Mindestens 1 Ziffer", satisfied: false },
      { code: "SPECIAL_CHAR", label: "Mindestens 1 Sonderzeichen", satisfied: false }
    ]);
  });
});
