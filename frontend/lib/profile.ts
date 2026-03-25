export type Profile = {
    username: string;
    email: string;
    firstName?: string | null;
    lastName?: string | null;
};

export type UpdateProfilePayload = {
    username: string;
    firstName?: string;
    lastName?: string;
};

export type ChangePasswordPayload = {
    newPassword: string;
    newPasswordRepeat: string;
};

export type DeleteAccountPayload = {
    confirmation: string;
};

export type ApiError = {
    code?: string;
    message?: string;
    field?: string | null;
};

export type MessageResponse = {
    message?: string;
};
