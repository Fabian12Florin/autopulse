interface PagedContent<T> {
    content?: T[];
    totalPages?: number;
}

export async function loadAllPages<T>(
    loadPage: (pageIndex: number, size: number, signal?: AbortSignal) => Promise<PagedContent<T>>,
    signal: AbortSignal,
    size: number = 500
) {
    const rows: T[] = [];
    let pageIndex = 0;
    let totalPages = 1;

    while (pageIndex < totalPages) {
        const page = await loadPage(pageIndex, size, signal);
        rows.push(...(page.content ?? []));
        totalPages = Math.max(page.totalPages ?? 1, 1);
        pageIndex += 1;

        if (signal.aborted) {
            break;
        }
    }

    return rows;
}
