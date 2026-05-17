import {env} from "@/env";

type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export const ACCESS_TOKEN_STORAGE_KEY = "autopulse_access_token";
export const REFRESH_TOKEN_STORAGE_KEY = "autopulse_refresh_token";
export const API_UNAUTHORIZED_EVENT = "autopulse:api-unauthorized";
let lastUnauthorizedEventAt = 0;

interface ApiRequestOptions {
    method?: HttpMethod;
    body?: unknown;
    headers?: Record<string, string>;
    signal?: AbortSignal;
    auth?: boolean;
}

interface ApiErrorPayload {
    message?: string;
    error?: string;
    code?: string;
    details?: Record<string, unknown>;
}

export class ApiError extends Error {
    status: number;
    code?: string;
    details?: Record<string, unknown>;

    constructor(status: number, message: string, code?: string, details?: Record<string, unknown>) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.code = code;
        this.details = details;
    }
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
    const token = localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
    const headers: Record<string, string> = {
        "Content-Type": "application/json",
        ...options.headers
    };

    if (options.auth !== false && token) {
        headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(buildApiUrl(path), {
        method: options.method ?? "GET",
        headers,
        body: options.body ? JSON.stringify(options.body) : undefined,
        signal: options.signal
    });

    if (!response.ok) {
        if (response.status === 401 && options.auth !== false) {
            emitUnauthorizedEvent();
        }
        throw await buildApiError(response);
    }

    if (response.status === 204) {
        return undefined as T;
    }

    return (await response.json()) as T;
}

function emitUnauthorizedEvent() {
    const now = Date.now();
    if (now - lastUnauthorizedEventAt < 1000) {
        return;
    }

    lastUnauthorizedEventAt = now;
    window.dispatchEvent(new CustomEvent(API_UNAUTHORIZED_EVENT));
}

function buildApiUrl(path: string) {
    const normalizedPath = path.startsWith("/") ? path : `/${path}`;
    const baseHasApiPrefix = env.apiBaseUrl.endsWith("/api");
    const pathHasApiPrefix = normalizedPath === "/api" || normalizedPath.startsWith("/api/");

    if (baseHasApiPrefix && pathHasApiPrefix) {
        return `${env.apiBaseUrl}${normalizedPath.slice("/api".length)}`;
    }

    if (!baseHasApiPrefix && !pathHasApiPrefix) {
        return `${env.apiBaseUrl}/api${normalizedPath}`;
    }

    return `${env.apiBaseUrl}${normalizedPath}`;
}

async function buildApiError(response: Response) {
    const fallbackMessage = `API request failed with status ${response.status}`;

    try {
        const payload = (await response.json()) as ApiErrorPayload;
        return new ApiError(
            response.status,
            payload.message || payload.error || fallbackMessage,
            payload.code,
            payload.details
        );
    } catch {
        return new ApiError(response.status, fallbackMessage);
    }
}
