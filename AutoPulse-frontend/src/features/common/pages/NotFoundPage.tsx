import {Link} from "react-router-dom";
import {ThemeToggleButton} from "@/components/ThemeToggleButton";

export function NotFoundPage() {
    return (
        <div className="auth-shell">
            <ThemeToggleButton className="auth-theme-toggle"/>
            <section className="auth-card not-found-card">
                <h1>404: Page Not Found</h1>
                <p className="auth-subtitle">The page you are looking for is unavailable.</p>
                <Link className="primary-btn" to="/dashboard">
                    Go to Dashboard
                </Link>
            </section>
        </div>
    );
}
