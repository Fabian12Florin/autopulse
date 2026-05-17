export function errorMessage(error: unknown, fallback: string) {
    return error instanceof Error ? error.message : fallback;
}

export function shortId(id: string, length: number = 8) {
    return id.slice(0, length);
}
