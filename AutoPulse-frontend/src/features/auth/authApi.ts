import {apiRequest} from "@/api/httpClient";

export interface TokenResponse {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
    refreshExpiresIn: number;
    tokenType: string;
    scope: string;
    sessionState: string;
}

export interface UserResponse {
    id: string;
    keycloakUserId: string;
    dispatcherProfileId: string | null;
    courierProfileId: string | null;
    email: string;
    firstName: string | null;
    lastName: string | null;
    phoneNumber: string | null;
    active: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface PasswordResetAcceptedResponse {
    message: string;
}

export function loginRequest(email: string, password: string) {
    return apiRequest<TokenResponse>("/users/auth/login", {
        method: "POST",
        auth: false,
        body: {email, password}
    });
}

export function refreshTokenRequest(refreshToken: string) {
    return apiRequest<TokenResponse>("/users/auth/refresh", {
        method: "POST",
        auth: false,
        body: {refreshToken}
    });
}

export function logoutRequest(refreshToken: string) {
    return apiRequest<void>("/users/auth/logout", {
        method: "POST",
        auth: false,
        body: {refreshToken}
    });
}

export function passwordResetRequest(email: string) {
    return apiRequest<PasswordResetAcceptedResponse>("/users/auth/reset-password", {
        method: "POST",
        auth: false,
        body: {email}
    });
}

export function getCurrentUserRequest() {
    return apiRequest<UserResponse>("/users/me");
}
