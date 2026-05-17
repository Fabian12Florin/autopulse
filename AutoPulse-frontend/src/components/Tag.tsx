interface TagProps {
    text: string;
    tone?: "success" | "warning" | "danger" | "neutral";
}

export function Tag({text, tone = "neutral"}: TagProps) {
    return <span className={`tag ${tone}`}>{text}</span>;
}
