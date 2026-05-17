import type {ReactNode} from "react";

interface TableCardProps {
    title: string;
    caption: string;
    columns: string[];
    rows: Array<Array<ReactNode>>;
    footer?: ReactNode;
    headerAction?: ReactNode;
    loading?: boolean;
}

export function TableCard({title, caption, columns, rows, footer, headerAction, loading = false}: TableCardProps) {
    return (
        <section className={`table-card${loading ? " loading" : ""}`}>
            <div className="table-card-head">
                <div className="table-card-head-row">
                    <div>
                        <h3>{title}</h3>
                        <p>{caption}</p>
                    </div>
                    {headerAction}
                </div>
            </div>
            <div className="table-wrap">
                <table>
                    <thead>
                    <tr>
                        {columns.map((column) => (
                            <th key={column}>{column}</th>
                        ))}
                    </tr>
                    </thead>
                    <tbody>
                    {rows.map((row, rowIndex) => (
                        <tr key={`row-${rowIndex}`}>
                            {row.map((cell, cellIndex) => (
                                <td key={`cell-${rowIndex}-${cellIndex}`}>{cell}</td>
                            ))}
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
            {loading ? <div className="loading-overlay" aria-hidden="true"/> : null}
            {footer ? <div className="table-card-footer">{footer}</div> : null}
        </section>
    );
}
