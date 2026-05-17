import {createContext, type ReactNode, useContext, useEffect, useMemo, useState} from "react";

export type AppTheme = "dark" | "light";

const THEME_STORAGE_KEY = "autopulse_theme";

interface ThemeContextValue {
    theme: AppTheme;
    setTheme: (theme: AppTheme) => void;
    toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

function readStoredTheme(): AppTheme | null {
    const value = window.localStorage.getItem(THEME_STORAGE_KEY);
    return value === "dark" || value === "light" ? value : null;
}

function detectPreferredTheme(): AppTheme {
    return window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark";
}

function resolveInitialTheme(): AppTheme {
    if (typeof window === "undefined") {
        return "dark";
    }

    return readStoredTheme() ?? detectPreferredTheme();
}

function applyTheme(theme: AppTheme) {
    document.documentElement.setAttribute("data-theme", theme);
    document.documentElement.style.colorScheme = theme;
}

export function ThemeProvider({children}: { children: ReactNode }) {
    const [theme, setTheme] = useState<AppTheme>(resolveInitialTheme);

    useEffect(() => {
        applyTheme(theme);
        window.localStorage.setItem(THEME_STORAGE_KEY, theme);
    }, [theme]);

    const value = useMemo<ThemeContextValue>(() => ({
        theme,
        setTheme,
        toggleTheme: () => setTheme((current) => (current === "dark" ? "light" : "dark"))
    }), [theme]);

    return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
    const context = useContext(ThemeContext);

    if (!context) {
        throw new Error("useTheme must be used inside ThemeProvider.");
    }

    return context;
}
