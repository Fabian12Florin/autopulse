import type {ReactNode} from "react";

interface PageHeaderProps {
    title: string;
    subtitle: string;
    actions?: ReactNode;
}

export function PageHeader({title, subtitle, actions}: PageHeaderProps) {
    return (
        <header className="page-header">
            <div>
                <h2>{title}</h2>
                <p>{subtitle}</p>
            </div>
            {actions ? <div>{actions}</div> : null}
        </header>
    );
}
