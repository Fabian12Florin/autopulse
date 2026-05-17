const FALLBACK_API_URL = "http://localhost:8080/api";

function normalizeBaseUrl(value: string | undefined) {
    const url = value?.trim() || FALLBACK_API_URL;
    return url.replace(/\/+$/, "");
}

export const env = {
    apiBaseUrl: normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL)
};
