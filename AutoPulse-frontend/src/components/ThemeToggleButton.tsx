import {useTheme} from "@/features/common/theme/ThemeContext";

interface ThemeToggleButtonProps {
    className?: string;
}

export function ThemeToggleButton({className}: ThemeToggleButtonProps) {
    const {theme, toggleTheme} = useTheme();
    const nextTheme = theme === "dark" ? "light" : "dark";

    return (
        <button
            type="button"
            className={`theme-toggle-btn${className ? ` ${className}` : ""}`}
            onClick={toggleTheme}
            aria-label={`Switch to ${nextTheme} theme`}
            title={`Switch to ${nextTheme} theme`}
        >
            <span className="theme-toggle-btn-state">{theme === "dark" ? "Dark" : "Light"}</span>
            <span className="theme-toggle-btn-action">Switch</span>
        </button>
    );
}
