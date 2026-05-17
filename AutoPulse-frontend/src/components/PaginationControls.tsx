interface PaginationPage {
    number: number;
    totalPages: number;
    first: boolean;
    last: boolean;
}

interface PaginationControlsProps {
    page: PaginationPage;
    onPageChange: (page: number) => void;
}

export function PaginationControls({page, onPageChange}: PaginationControlsProps) {
    return (
        <div className="pagination-row">
            <button type="button" className="secondary-btn" onClick={() => onPageChange(page.number - 1)}
                    disabled={page.first}>
                Previous
            </button>
            <span>
                Page {page.number + 1} of {Math.max(page.totalPages, 1)}
            </span>
            <button type="button" className="secondary-btn" onClick={() => onPageChange(page.number + 1)}
                    disabled={page.last || page.totalPages === 0}>
                Next
            </button>
        </div>
    );
}
