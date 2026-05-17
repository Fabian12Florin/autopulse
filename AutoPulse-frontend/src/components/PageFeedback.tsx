interface PageFeedbackProps {
    error?: string | null;
    notice?: string | null;
    errors?: string[];
}

export function PageFeedback({error, notice, errors = []}: PageFeedbackProps) {
    const uniqueErrors = Array.from(new Set(errors.filter((value) => value.trim().length > 0)));
    const hasAny = Boolean(error) || Boolean(notice) || uniqueErrors.length > 0;

    if (!hasAny) {
        return null;
    }

    return (
        <section className="page-feedback" aria-live="polite">
            {error ? <p className="page-feedback-item error">{error}</p> : null}
            {uniqueErrors.map((item) => (
                <p key={item} className="page-feedback-item error">{item}</p>
            ))}
            {notice ? <p className="page-feedback-item success">{notice}</p> : null}
        </section>
    );
}
