import React from "react";
import { HttpResponse, http } from "msw";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { RegistrationForm } from "@/components/registration-form";
import { registrationPolicyFixture } from "@/test/fixtures/registration-policy";
import { server } from "@/test/msw/server";

describe("RegistrationForm", () => {
  it("loads the registration policy and renders helper content", async () => {
    server.use(
      http.get("/api/registration/policy", () => HttpResponse.json(registrationPolicyFixture))
    );

    render(<RegistrationForm />);

    expect(await screen.findByRole("heading", { name: /Erstelle dein drumdibum Konto/i })).toBeInTheDocument();
    expect(screen.getByText(/Bitte bestaetige die Sicherheitspruefung/i)).toBeInTheDocument();
    expect(screen.queryByText(/Lokaler Testmodus/i)).not.toBeInTheDocument();
    expect(screen.getByText(/Version 2026-03/i)).toBeInTheDocument();
  });

  it("shows nickname suggestions from the backend and applies them", async () => {
    server.use(
      http.get("/api/registration/policy", () => HttpResponse.json(registrationPolicyFixture)),
      http.post(
        "/api/registration",
        () =>
          HttpResponse.json(
            {
              code: "NICKNAME_ALREADY_EXISTS",
              message: "Nickname existiert bereits",
              field: "nickname",
              suggestedNickname: "drummer2"
            },
            { status: 409 }
          )
      )
    );

    const user = userEvent.setup();
    render(<RegistrationForm />);

    await screen.findByRole("heading", { name: /Erstelle dein drumdibum Konto/i });
    await user.type(screen.getByLabelText("Nickname"), "drummer");
    await user.type(screen.getByLabelText("Email-Adresse"), "drummer@example.org");
    await user.type(screen.getByLabelText("Passwort"), "Drum123!");
    await user.type(screen.getByLabelText("Passwort wiederholen"), "Drum123!");
    await user.type(screen.getByLabelText("Captcha"), "test-pass");
    await user.click(screen.getByLabelText(/Ich stimme den/i));
    await user.click(screen.getByRole("button", { name: /Registrieren/i }));

    const suggestionButton = await screen.findByRole("button", { name: /Vorschlag uebernehmen: drummer2/i });
    await user.click(suggestionButton);

    await waitFor(() => {
      expect(screen.getByLabelText("Nickname")).toHaveValue("drummer2");
    });
  });

  it("submits successfully and resets the form", async () => {
    const registrationSpy = vi.fn();

    server.use(
      http.get("/api/registration/policy", () => HttpResponse.json(registrationPolicyFixture)),
      http.post("/api/registration", async ({ request }) => {
        registrationSpy(await request.json());
        return HttpResponse.json({ message: "Die Verifizierungs-E-Mail wurde versendet" }, { status: 202 });
      })
    );

    const user = userEvent.setup();
    render(<RegistrationForm />);

    await screen.findByRole("heading", { name: /Erstelle dein drumdibum Konto/i });
    await user.type(screen.getByLabelText("Vorname"), "Max");
    await user.type(screen.getByLabelText("Name"), "Mustermann");
    await user.type(screen.getByLabelText("Nickname"), "drummer");
    await user.type(screen.getByLabelText("Email-Adresse"), "drummer@example.org");
    await user.type(screen.getByLabelText("Passwort"), "Drum123!");
    await user.type(screen.getByLabelText("Passwort wiederholen"), "Drum123!");
    await user.type(screen.getByLabelText("Captcha"), "test-pass");
    await user.click(screen.getByLabelText(/Ich stimme den/i));
    await user.click(screen.getByRole("button", { name: /Registrieren/i }));

    expect(await screen.findByText(/Verifizierungs-E-Mail wurde versendet/i)).toBeInTheDocument();
    expect(registrationSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        nickname: "drummer",
        email: "drummer@example.org",
        acceptTerms: true
      })
    );
    expect(screen.getByLabelText("Nickname")).toHaveValue("");
  });
});
