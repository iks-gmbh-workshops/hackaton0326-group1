"use client";

import Link from "next/link";
import { signOut } from "next-auth/react";
import { useDeferredValue, useEffect, useState } from "react";
import { PasswordRequirements } from "@/components/password-requirements";
import type { ApiError, ChangePasswordPayload, DeleteAccountPayload, Profile, UpdateProfilePayload } from "@/lib/profile";
import { evaluatePassword, type RegistrationPolicy, validateNickname } from "@/lib/registration";

const initialProfileForm: UpdateProfilePayload = {
    username: "",
    firstName: "",
    lastName: ""
};

const initialPasswordForm: ChangePasswordPayload = {
    newPassword: "",
    newPasswordRepeat: ""
};

const initialDeleteForm: DeleteAccountPayload = {
    confirmation: ""
};

export function ProfileManagement() {
    const [profile, setProfile] = useState<Profile | null>(null);
    const [profileForm, setProfileForm] = useState<UpdateProfilePayload>(initialProfileForm);
    const [passwordForm, setPasswordForm] = useState<ChangePasswordPayload>(initialPasswordForm);
    const [deleteForm, setDeleteForm] = useState<DeleteAccountPayload>(initialDeleteForm);
    const [policy, setPolicy] = useState<RegistrationPolicy | null>(null);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [profileError, setProfileError] = useState<ApiError | null>(null);
    const [passwordError, setPasswordError] = useState<ApiError | null>(null);
    const [deleteError, setDeleteError] = useState<ApiError | null>(null);
    const [profileSuccess, setProfileSuccess] = useState<string | null>(null);
    const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);
    const [deleteSuccess, setDeleteSuccess] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSavingProfile, setIsSavingProfile] = useState(false);
    const [isChangingPassword, setIsChangingPassword] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const deferredPassword = useDeferredValue(passwordForm.newPassword);

    useEffect(() => {
        let ignore = false;

        async function load() {
            try {
                const responses = await Promise.all([
                    fetch("/api/profile", { cache: "no-store" }),
                    fetch("/api/registration/policy", { cache: "no-store" })
                ]);

                const profileResponse = responses[0];
                const policyResponse = responses[1];

                const profileBody = (await profileResponse.json()) as Profile & ApiError;
                const policyBody = (await policyResponse.json()) as RegistrationPolicy & ApiError;

                if (ignore) {
                    return;
                }

                if (profileResponse.ok === false) {
                    setLoadError(profileBody.message ?? "Profil konnte nicht geladen werden");
                    setIsLoading(false);
                    return;
                }

                if (policyResponse.ok === false) {
                    setLoadError(policyBody.message ?? "Passwortregeln konnten nicht geladen werden");
                    setIsLoading(false);
                    return;
                }

                setProfile(profileBody);
                setPolicy(policyBody);
                setProfileForm({
                    username: profileBody.username,
                    firstName: profileBody.firstName ?? "",
                    lastName: profileBody.lastName ?? ""
                });
                setDeleteForm({ confirmation: "" });
                setIsLoading(false);
            } catch (error) {
                if (ignore) {
                    return;
                }

                setLoadError(error instanceof Error ? error.message : "Profil konnte nicht geladen werden");
                setIsLoading(false);
            }
        }

        void load();

        return () => {
            ignore = true;
        };
    }, []);

    const nicknameValidationError =
        policy == null ? null : validateNickname(profileForm.username, policy.nickname);
    const passwordRequirements =
        policy == null ? [] : evaluatePassword(deferredPassword, policy.password);

    function updateProfileField<K extends keyof UpdateProfilePayload>(field: K, value: UpdateProfilePayload[K]) {
        setProfileForm((current) => ({ ...current, [field]: value }));
        setProfileError((current) => (current?.field === field ? null : current));
        setProfileSuccess(null);
    }

    function updatePasswordField<K extends keyof ChangePasswordPayload>(field: K, value: ChangePasswordPayload[K]) {
        setPasswordForm((current) => ({ ...current, [field]: value }));
        setPasswordError((current) => (current?.field === field ? null : current));
        setPasswordSuccess(null);
    }

    function updateDeleteField(value: string) {
        setDeleteForm({ confirmation: value });
        setDeleteError(null);
        setDeleteSuccess(null);
    }

    async function handleProfileSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setProfileSuccess(null);

        if (nicknameValidationError) {
            setProfileError({
                code: "INVALID_USERNAME",
                message: nicknameValidationError,
                field: "username"
            });
            return;
        }

        setIsSavingProfile(true);

        try {
            const response = await fetch("/api/profile", {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(profileForm)
            });

            const body = (await response.json()) as Profile & ApiError;

            if (response.ok === false) {
                setProfileError({
                    code: body.code,
                    message: body.message,
                    field: body.field
                });
                return;
            }

            setProfile(body);
            setProfileForm({
                username: body.username,
                firstName: body.firstName ?? "",
                lastName: body.lastName ?? ""
            });
            setProfileError(null);
            setProfileSuccess("Profil wurde gespeichert");
        } catch (error) {
            setProfileError({
                code: "NETWORK_ERROR",
                message: error instanceof Error ? error.message : "Profil konnte nicht gespeichert werden"
            });
        } finally {
            setIsSavingProfile(false);
        }
    }

    async function handlePasswordSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setPasswordSuccess(null);
        setIsChangingPassword(true);

        try {
            const response = await fetch("/api/profile/password", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(passwordForm)
            });

            const body = (await response.json()) as ApiError & { message?: string };

            if (response.ok === false) {
                setPasswordError({
                    code: body.code,
                    message: body.message,
                    field: body.field
                });
                return;
            }

            setPasswordError(null);
            setPasswordSuccess(body.message ?? "Passwort wurde geändert");
            setPasswordForm(initialPasswordForm);
        } catch (error) {
            setPasswordError({
                code: "NETWORK_ERROR",
                message: error instanceof Error ? error.message : "Passwort konnte nicht geändert werden"
            });
        } finally {
            setIsChangingPassword(false);
        }
    }

    async function handleDeleteSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setDeleteSuccess(null);
        setIsDeleting(true);

        try {
            const response = await fetch("/api/profile", {
                method: "DELETE",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(deleteForm)
            });

            const body = (await response.json()) as ApiError & { message?: string };

            if (response.ok === false) {
                setDeleteError({
                    code: body.code,
                    message: body.message,
                    field: body.field
                });
                return;
            }

            setDeleteError(null);
            setDeleteSuccess(body.message ?? "Konto wurde gelöscht");

            const logoutResponse = await fetch("/api/auth/federated-logout", {
                method: "GET",
                cache: "no-store"
            });
            const logoutBody = logoutResponse.ok ? ((await logoutResponse.json()) as { logoutUrl?: string }) : {};

            await signOut({ redirect: false });

            if (logoutBody.logoutUrl) {
                window.location.assign(logoutBody.logoutUrl);
                return;
            }

            window.location.assign("/");
        } catch (error) {
            setDeleteError({
                code: "NETWORK_ERROR",
                message: error instanceof Error ? error.message : "Konto konnte nicht gelöscht werden"
            });
        } finally {
            setIsDeleting(false);
        }
    }

    if (isLoading) {
        return <div className="soft-panel">Lade Profil...</div>;
    }

    if (loadError || profile == null || policy == null) {
        return <div className="alert alert-error">{loadError ?? "Profil konnte nicht geladen werden"}</div>;
    }

    return (
        <main className="page-shell">
            <div className="page-container space-y-8">
                <section className="brand-card card">
                    <div className="card-body gap-4">
                        <div className="flex flex-wrap items-center justify-between gap-4">
                            <div className="section-intro">
                                <p className="section-title">Profil</p>
                                <h1 className="section-headline text-[2.4rem]">Konto verwalten</h1>
                                <p className="subheadline">
                                    Aktualisiere deinen Username und deine Stammdaten, ändere dein Passwort oder lösche dein Konto vollständig.
                                </p>
                            </div>
                            <Link className="btn btn-ghost" href="/">
                                Zur Startseite
                            </Link>
                        </div>
                    </div>
                </section>

                <section className="grid gap-6 xl:grid-cols-[1.08fr_0.92fr]">
                    <form className="brand-card space-y-5 p-6" onSubmit={handleProfileSubmit}>
                        <div className="space-y-2">
                            <p className="section-title">Profil bearbeiten</p>
                            <h2 className="section-headline text-[2rem]">Sichtbare Kontodaten</h2>
                            <p className="subheadline">
                                Username, Vorname und Nachname werden nach dem Speichern auch in Keycloak synchronisiert.
                            </p>
                        </div>

                        <Field
                            error={profileError?.field === "username" ? profileError.message : nicknameValidationError ?? undefined}
                            id="username"
                            label="Username"
                            onChange={(value) => updateProfileField("username", value)}
                            required
                            value={profileForm.username}
                        />

                        <Field
                            disabled
                            id="email"
                            label="Email-Adresse"
                            value={profile.email}
                        />

                        <div className="grid gap-4 sm:grid-cols-2">
                            <Field
                                error={profileError?.field === "firstName" ? profileError.message : undefined}
                                id="firstName"
                                label="Vorname"
                                onChange={(value) => updateProfileField("firstName", value)}
                                value={profileForm.firstName ?? ""}
                            />
                            <Field
                                error={profileError?.field === "lastName" ? profileError.message : undefined}
                                id="lastName"
                                label="Nachname"
                                onChange={(value) => updateProfileField("lastName", value)}
                                value={profileForm.lastName ?? ""}
                            />
                        </div>

                        {profileError && profileError.field == null ? (
                            <div className="alert alert-error">{profileError.message}</div>
                        ) : null}
                        {profileSuccess ? <div className="alert alert-success">{profileSuccess}</div> : null}

                        <button className="btn btn-primary" disabled={isSavingProfile} type="submit">
                            {isSavingProfile ? "Speichere..." : "Profil speichern"}
                        </button>
                    </form>

                    <div className="space-y-6">
                        <form className="brand-card space-y-5 p-6" onSubmit={handlePasswordSubmit}>
                            <div className="space-y-2">
                                <p className="section-title">Passwort ändern</p>
                                <h2 className="section-headline text-[2rem]">Neues Passwort setzen</h2>
                                <p className="subheadline">Lege ein neues Passwort fest. Du bist bereits angemeldet, daher ist keine Verifizierung des aktuellen Passworts nötig.</p>
                            </div>

                            <div className="grid gap-4 sm:grid-cols-2">
                                <Field
                                    error={passwordError?.field === "newPassword" ? passwordError.message : undefined}
                                    id="newPassword"
                                    label="Neues Passwort"
                                    onChange={(value) => updatePasswordField("newPassword", value)}
                                    required
                                    type="password"
                                    value={passwordForm.newPassword}
                                />
                                <Field
                                    error={passwordError?.field === "newPasswordRepeat" ? passwordError.message : undefined}
                                    id="newPasswordRepeat"
                                    label="Passwort wiederholen"
                                    onChange={(value) => updatePasswordField("newPasswordRepeat", value)}
                                    required
                                    type="password"
                                    value={passwordForm.newPasswordRepeat}
                                />
                            </div>

                            {passwordError && passwordError.field == null ? (
                                <div className="alert alert-error">{passwordError.message}</div>
                            ) : null}
                            {passwordSuccess ? <div className="alert alert-success">{passwordSuccess}</div> : null}

                            <button className="btn btn-primary" disabled={isChangingPassword} type="submit">
                                {isChangingPassword ? "Ändere..." : "Passwort ändern"}
                            </button>
                        </form>

                        <PasswordRequirements requirements={passwordRequirements} />

                        <form
                            className="brand-card !border-red-950/45 !bg-[linear-gradient(180deg,#c62828_0%,#a91b1b_100%)] !text-white space-y-5 p-6 shadow-[0_24px_60px_rgba(127,29,29,0.32)]"
                            onSubmit={handleDeleteSubmit}
                        >
                            <div className="space-y-4">
                                <span className="inline-flex w-fit items-center rounded-full bg-red-950/45 px-3 py-1 text-[0.72rem] font-semibold uppercase tracking-[0.24em] text-white shadow-sm">
                                    Danger Zone
                                </span>

                                <div className="space-y-2">
                                    <p className="section-title !text-white/80">Konto löschen</p>
                                    <h2 className="section-headline !text-white text-[2rem]">Unwiderruflich entfernen</h2>
                                    <p className="subheadline max-w-2xl !text-white/90">
                                        Dieser Schritt ist endgültig. Wenn du dein Konto löschst, werden deine Zugangsdaten und Profildaten dauerhaft entfernt und können nicht wiederhergestellt werden.
                                    </p>
                                    <p className="text-sm font-semibold leading-7 text-white/95">
                                        Dein Konto wird sowohl in Keycloak als auch in der App-Datenbank gelöscht. Zur Bestätigung musst du deinen aktuellen Username exakt eingeben: {profile.username}
                                    </p>
                                </div>
                            </div>

                            <label className="form-control gap-2" htmlFor="confirmation">
                                <span className="label-text font-medium !text-white">Username zur Löschung bestätigen</span>
                                <input
                                    className={`input input-bordered w-full border-white/20 bg-white text-base-content placeholder:text-base-content/45 ${deleteError?.field === "confirmation" ? "input-error" : ""}`}
                                    id="confirmation"
                                    onChange={(event) => updateDeleteField(event.target.value)}
                                    required
                                    value={deleteForm.confirmation}
                                />
                                {deleteError?.field === "confirmation" ? <span className="text-sm text-white">{deleteError.message}</span> : null}
                            </label>

                            {deleteError && deleteError.field == null ? (
                                <div className="rounded-[1rem] border border-white/20 bg-white/12 px-4 py-3 text-sm text-white shadow-sm">{deleteError.message}</div>
                            ) : null}
                            {deleteSuccess ? <div className="alert alert-success shadow-sm">{deleteSuccess}</div> : null}

                            <p className="text-sm leading-6 text-white/82">
                                Bitte lösche dein Konto nur, wenn du dir sicher bist, dass dieser Schritt dauerhaft sein soll.
                            </p>

                            <button className="btn border-none bg-white text-red-800 shadow-sm hover:bg-white/92 sm:min-w-52" disabled={isDeleting} type="submit">
                                {isDeleting ? "Lösche unwiderruflich..." : "Konto jetzt endgültig löschen"}
                            </button>
                        </form>
                    </div>
                </section>
            </div>
        </main>
    );
}

type FieldProps = {
    disabled?: boolean;
    error?: string;
    id: string;
    label: string;
    onChange?: (value: string) => void;
    required?: boolean;
    type?: string;
    value: string;
};

function Field({ disabled, error, id, label, onChange, required, type = "text", value }: FieldProps) {
    return (
        <label className="form-control gap-2" htmlFor={id}>
            <span className="label-text font-medium">{label}</span>
            <input
                className={`input input-bordered w-full ${error ? "input-error" : ""}`}
                disabled={disabled}
                id={id}
                onChange={(event) => onChange?.(event.target.value)}
                required={required}
                type={type}
                value={value}
            />
            {error ? <span className="text-sm text-error">{error}</span> : null}
        </label>
    );
}
