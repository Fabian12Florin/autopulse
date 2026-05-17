interface StatCardProps {
    label: string;
    value: string;
    trend?: string;
    tone?: "positive" | "negative" | "neutral" | "warning";
    onClick?: () => void;
}

export function StatCard({label, value, trend, tone = "neutral", onClick}: StatCardProps) {
    const className = `stat-card${onClick ? " stat-card-action" : ""}`;

    if (onClick) {
        return (
            <button type="button" className={className} onClick={onClick}>
                <p className="stat-label">{label}</p>
                <p className="stat-value">{value}</p>
                {trend ? <p className={`stat-trend ${tone}`}>{trend}</p> : null}
            </button>
        );
    }

    return (
        <article className={className}>
            <p className="stat-label">{label}</p>
            <p className="stat-value">{value}</p>
            {trend ? <p className={`stat-trend ${tone}`}>{trend}</p> : null}
        </article>
    );
}
