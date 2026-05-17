import {createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState} from "react";
import {ACCESS_TOKEN_STORAGE_KEY, API_UNAUTHORIZED_EVENT, REFRESH_TOKEN_STORAGE_KEY} from "@/api/httpClient";
import {
    getCurrentUserRequest,
    loginRequest,
    logoutRequest,
    refreshTokenRequest,
    type TokenResponse,
    type UserResponse
} from "@/features/auth/authApi";

const LEGACY_TOKEN_KEY = "autopulse_token";
const REFRESH_SKEW_SECONDS = 45;

export type PortalRole = "ADMIN" | "DISPATCHER" | "COURIER" | "UNASSIGNED";
type AuthStatus = "checking" | "authenticated" | "anonymous";

export interface SessionUser {
    id: string;
    email: string;
    fullName: string;
    role: PortalRole;
    roles: PortalRole[];
    depotId?: string;
    regionCode?: string;
    dispatcherProfileId?: string;
    courierProfileId?: string;
}

interface AuthContextValue {
    user: SessionUser | null;
    status: AuthStatus;
    isAuthenticated: boolean;
    login: (email: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
}

interface JwtPayload {
    exp?: number;
    email?: string;
    name?: string;
    given_name?: string;
    family_name?: string;
    preferred_username?: string;
    depotId?: string;
    depot_id?: string;
    regionCode?: string;
    region_code?: string;
    dispatcherProfileId?: string;
    dispatcher_profile_id?: string;
    courierProfileId?: string;
    courier_profile_id?: string;
    realm_access?: {
        roles?: string[];
    };
    resource_access?: Record<string, { roles?: string[] }>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({children}: { children: ReactNode }) {
    const [user, setUser] = useState<SessionUser | null>(null);
    const [status, setStatus] = useState<AuthStatus>("checking");

    const clearStoredSession = useCallback(() => {
        localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
        localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
        localStorage.removeItem(LEGACY_TOKEN_KEY);
        localStorage.removeItem("autopulse_role");
    }, []);

    const clearSession = useCallback(() => {
        clearStoredSession();
        setUser(null);
        setStatus("anonymous");
    }, [clearStoredSession]);

    const establishSession = useCallback(
        async (tokens: TokenResponse) => {
            try {
                persistTokens(tokens);
                const payload = decodeJwt(tokens.accessToken);
                const me = await getCurrentUserRequest();
                setUser(buildSessionUser(me, payload));
                setStatus("authenticated");
            } catch (error) {
                clearSession();
                throw error;
            }
        },
        [clearSession]
    );

    useEffect(() => {
        let isMounted = true;

        async function bootstrapSession() {
            const accessToken = localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
            const refreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);

            if (!accessToken && !refreshToken) {
                clearSession();
                return;
            }

            try {
                const nextTokens = shouldRefresh(accessToken) && refreshToken
                    ? await refreshTokenRequest(refreshToken)
                    : null;

                if (!isMounted) {
                    return;
                }

                if (nextTokens) {
                    await establishSession(nextTokens);
                    return;
                }

                const payload = accessToken ? decodeJwt(accessToken) : null;
                const me = await getCurrentUserRequest();

                if (!isMounted) {
                    return;
                }

                setUser(buildSessionUser(me, payload));
                setStatus("authenticated");
            } catch {
                if (isMounted) {
                    clearSession();
                }
            }
        }

        void bootstrapSession();

        return () => {
            isMounted = false;
        };
    }, [clearSession, establishSession]);

    useEffect(() => {
        function handleUnauthorized() {
            const path = window.location.pathname;
            if (path === "/login" || path === "/password-reset") {
                clearSession();
                return;
            }

            clearStoredSession();
            const redirectTarget = `${path}${window.location.search}${window.location.hash}`;
            const next = `/login?reason=session-expired&redirect=${encodeURIComponent(redirectTarget)}`;
            window.location.replace(next);
        }

        window.addEventListener(API_UNAUTHORIZED_EVENT, handleUnauthorized);
        return () => window.removeEventListener(API_UNAUTHORIZED_EVENT, handleUnauthorized);
    }, [clearSession, clearStoredSession]);

    const login = useCallback(
        async (email: string, password: string) => {
            const tokens = await loginRequest(email.trim(), password);
            await establishSession(tokens);
        },
        [establishSession]
    );

    const logout = useCallback(async () => {
        const refreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);

        try {
            if (refreshToken) {
                await logoutRequest(refreshToken);
            }
        } finally {
            clearSession();
        }
    }, [clearSession]);

    const value = useMemo<AuthContextValue>(
        () => ({
            user,
            status,
            isAuthenticated: status === "authenticated" && Boolean(user),
            login,
            logout
        }),
        [login, logout, status, user]
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }

    return context;
}

function persistTokens(tokens: TokenResponse) {
    localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, tokens.refreshToken);
    localStorage.removeItem(LEGACY_TOKEN_KEY);
}

function shouldRefresh(accessToken: string | null) {
    if (!accessToken) {
        return true;
    }

    const payload = decodeJwt(accessToken);
    if (!payload?.exp) {
        return true;
    }

    return payload.exp - nowInSeconds() <= REFRESH_SKEW_SECONDS;
}

function buildSessionUser(user: UserResponse, payload: JwtPayload | null): SessionUser {
    const roles = extractPortalRoles(payload);
    const fullName = [user.firstName, user.lastName].filter(Boolean).join(" ").trim();

    return {
        id: user.id,
        email: user.email,
        fullName: fullName || payload?.name || payload?.preferred_username || user.email,
        role: roles[0] ?? "UNASSIGNED",
        roles,
        depotId: payload?.depotId ?? payload?.depot_id,
        regionCode: payload?.regionCode ?? payload?.region_code,
        dispatcherProfileId: user.dispatcherProfileId ?? undefined,
        courierProfileId: user.courierProfileId ?? undefined
    };
}

function extractPortalRoles(payload: JwtPayload | null): PortalRole[] {
    const rawRoles = new Set<string>();
    const portalRoles: PortalRole[] = ["ADMIN", "DISPATCHER", "COURIER"];

    payload?.realm_access?.roles?.forEach((role) => rawRoles.add(role));
    Object.values(payload?.resource_access ?? {}).forEach((resource) => {
        resource.roles?.forEach((role) => rawRoles.add(role));
    });

    const roles = Array.from(rawRoles)
        .map((role) => role.toUpperCase())
        .filter(isPortalRole);

    return portalRoles.filter((role) => roles.includes(role));
}

function isPortalRole(role: string): role is PortalRole {
    return role === "ADMIN" || role === "DISPATCHER" || role === "COURIER";
}

function decodeJwt(token: string): JwtPayload | null {
    const [, payload] = token.split(".");

    if (!payload) {
        return null;
    }

    try {
        const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
        const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
        return JSON.parse(atob(padded)) as JwtPayload;
    } catch {
        return null;
    }
}

function nowInSeconds() {
    return Math.floor(Date.now() / 1000);
}
