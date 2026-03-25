import { expect, test } from "@playwright/test";

const mailpitBaseUrl = process.env.MAILPIT_BASE_URL ?? "http://127.0.0.1:8025";
const keycloakUsername = process.env.E2E_DEMO_USERNAME ?? "demo@heuermannplus.local";
const keycloakPassword = process.env.E2E_DEMO_PASSWORD ?? "Demo123!";

async function waitForVerificationLink(email: string): Promise<string> {
  const deadline = Date.now() + 60_000;

  while (Date.now() < deadline) {
    const messagesResponse = await fetch(`${mailpitBaseUrl}/api/v1/messages`);

    if (messagesResponse.ok) {
      const payload = (await messagesResponse.json()) as { messages?: Array<{ ID?: string; To?: Array<{ Address?: string }> }> };
      const message = payload.messages?.find((entry) =>
        entry.To?.some((recipient) => recipient.Address?.toLowerCase() === email.toLowerCase())
      );

      if (message?.ID) {
        const messageResponse = await fetch(`${mailpitBaseUrl}/api/v1/message/${message.ID}`);

        if (messageResponse.ok) {
          const detail = (await messageResponse.json()) as {
            Text?: string;
            HTML?: string;
            text?: string;
            html?: string;
          };
          const content = [detail.Text, detail.HTML, detail.text, detail.html].filter(Boolean).join("\n");
          const match = content.match(/http:\/\/localhost:3000\/register\/verify\?token=[^\s"'<>]+/);

          if (match) {
            return match[0];
          }
        }
      }
    }

    await new Promise((resolve) => setTimeout(resolve, 2_000));
  }

  throw new Error(`No verification email found for ${email}`);
}

test.describe("registration and auth smoke flow", () => {
  test("loads the homepage", async ({ page }) => {
    await page.goto("/");

    await expect(page.getByRole("heading", { name: /Multilayer-Web-App mit klarer Trennung/i })).toBeVisible();
    await expect(page.getByRole("link", { name: /Jetzt registrieren/i })).toBeVisible();
  });

  test("registers, verifies, logs in and reaches the protected BFF flow", async ({ page }) => {
    const unique = `${Date.now()}`;
    const nickname = `tester${unique}`;
    const email = `tester.${unique}@heuermannplus.local`;

    await page.goto("/register");
    await page.getByLabel("Vorname").fill("Test");
    await page.getByLabel("Name").fill("Runner");
    await page.getByLabel("Nickname").fill(nickname);
    await page.getByLabel("Email-Adresse").fill(email);
    await page.getByLabel("Passwort").fill("Drum123!");
    await page.getByLabel("Passwort wiederholen").fill("Drum123!");
    await page.getByLabel("Captcha").fill("test-pass");
    await page.getByLabel(/Ich stimme den/i).check();
    await page.getByRole("button", { name: /Registrieren/i }).click();

    await expect(page.getByText(/Verifizierungs-E-Mail wurde versendet/i)).toBeVisible();

    const verificationLink = await waitForVerificationLink(email);
    await page.goto(verificationLink);
    await expect(page.getByText(/Registrierung wurde erfolgreich bestaetigt/i)).toBeVisible();

    await page.goto("/");
    await page.getByRole("button", { name: /Mit Keycloak anmelden/i }).click();
    await page.locator("input[name='username']").fill(keycloakUsername);
    await page.locator("input[name='password']").fill(keycloakPassword);
    await page.locator("button[type='submit']").click();

    await expect(page.getByText(/Willkommen/i)).toBeVisible();
    await page.getByRole("button", { name: /Geschuetzten Call ausfuehren/i }).click();
    await expect(page.locator("pre")).toContainText("demo@heuermannplus.local");
  });
});
