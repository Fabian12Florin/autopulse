import {type SubmitEventHandler, useState} from "react";
import {Link, Navigate, useLocation, useNavigate} from "react-router-dom";
import {useAuth} from "@/features/auth/AuthContext";
import {ThemeToggleButton} from "@/components/ThemeToggleButton";

export function LoginPage() {
    const {isAuthenticated, login, status} = useAuth();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();

    if (status === "checking") {
        return (
            <div className="auth-shell">
                <ThemeToggleButton className="auth-theme-toggle"/>
                <section className="auth-card">
                    <p className="brand-eyebrow">AutoPulse</p>
                    <h1>Checking session</h1>
                </section>
            </div>
        );
    }

    if (isAuthenticated) {
        return <Navigate to="/dashboard" replace/>;
    }

    const query = new URLSearchParams(location.search);
    const queryRedirect = query.get("redirect");
    const fromState = (location.state as { from?: string } | null)?.from;
    const from = sanitizeRedirectPath(fromState ?? queryRedirect) ?? "/dashboard";
    const sessionExpired = query.get("reason") === "session-expired";

    const handleSubmit: SubmitEventHandler<HTMLFormElement> = async (event) => {
        event.preventDefault();
        setError(null);
        setIsLoading(true);

        try {
            await login(email, password);
            navigate(from, {replace: true});
        } catch (loginError) {
            const message = loginError instanceof Error ? loginError.message : "Login failed.";
            setError(message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="auth-shell">
            <ThemeToggleButton className="auth-theme-toggle"/>
            <section className="auth-card">
                <p className="brand-eyebrow">AutoPulse</p>
                <h1>Operations Login</h1>
                <p className="auth-subtitle">
                    Sign in with your AutoPulse account.
                </p>
                {sessionExpired ? <p className="topbar-label">Your session expired. Please sign in again.</p> : null}

                <form onSubmit={handleSubmit} className="auth-form">
                    <label>
                        Email
                        <input
                            type="email"
                            autoComplete="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            maxLength={255}
                            required
                        />
                    </label>

                    <label>
                        Password
                        <input
                            type="password"
                            autoComplete="current-password"
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            minLength={8}
                            maxLength={255}
                            required
                        />
                    </label>

                    {error ? <p className="form-error">{error}</p> : null}

                    <button type="submit" className="primary-btn" disabled={isLoading}>
                        {isLoading ? "Signing in..." : "Login"}
                    </button>
                </form>

                <div className="auth-links">
                    <Link to="/password-reset">Forgot password?</Link>
                </div>
            </section>
        </div>
    );
}

function sanitizeRedirectPath(value: string | null | undefined) {
    if (!value) {
        return null;
    }

    if (!value.startsWith("/") || value.startsWith("//")) {
        return null;
    }

    return value;
}
