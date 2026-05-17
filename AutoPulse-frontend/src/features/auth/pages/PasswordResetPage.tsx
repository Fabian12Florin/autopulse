import {type SubmitEventHandler, useState} from "react";
import {Link} from "react-router-dom";
import {passwordResetRequest} from "@/features/auth/authApi";
import {ThemeToggleButton} from "@/components/ThemeToggleButton";

export function PasswordResetPage() {
    const [email, setEmail] = useState("");
    const [message, setMessage] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit: SubmitEventHandler<HTMLFormElement> = async (event) => {
        event.preventDefault();
        setError(null);
        setMessage(null);
        setIsLoading(true);

        try {
            const response = await passwordResetRequest(email.trim());
            setMessage(response.message || "If this email belongs to an active account, reset instructions will be sent.");
        } catch (resetError) {
            const resetMessage = resetError instanceof Error ? resetError.message : "Password reset request failed.";
            setError(resetMessage);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="auth-shell">
            <ThemeToggleButton className="auth-theme-toggle"/>
            <section className="auth-card">
                <p className="brand-eyebrow">AutoPulse</p>
                <h1>Password Reset</h1>
                <p className="auth-subtitle">Enter your work email and we will send reset instructions.</p>

                <form onSubmit={handleSubmit} className="auth-form">
                    <label>
                        Work email
                        <input
                            type="email"
                            autoComplete="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            maxLength={255}
                            required
                        />
                    </label>

                    {error ? <p className="form-error">{error}</p> : null}

                    <button type="submit" className="primary-btn" disabled={isLoading}>
                        {isLoading ? "Sending..." : "Send instructions"}
                    </button>
                </form>

                {message ? <p className="form-success">{message}</p> : null}

                <div className="auth-links">
                    <Link to="/login">Back to login</Link>
                </div>
            </section>
        </div>
    );
}
