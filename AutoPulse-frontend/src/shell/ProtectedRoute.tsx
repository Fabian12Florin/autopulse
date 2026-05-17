import {Navigate, useLocation} from "react-router-dom";
import type {ReactNode} from "react";
import {useAuth} from "@/features/auth/AuthContext";

export function ProtectedRoute({children}: { children: ReactNode }) {
    const {isAuthenticated, status} = useAuth();
    const location = useLocation();

    if (status === "checking") {
        return (
            <div className="app-loading">
                <p>Loading session...</p>
            </div>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace state={{from: location.pathname}}/>;
    }

    return <>{children}</>;
}
