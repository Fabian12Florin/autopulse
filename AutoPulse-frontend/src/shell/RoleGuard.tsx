import type {ReactNode} from "react";
import type {PortalRole} from "@/features/auth/AuthContext";
import {useAuth} from "@/features/auth/AuthContext";

interface RoleGuardProps {
    allowedRoles: PortalRole[];
    children: ReactNode;
}

export function RoleGuard({allowedRoles, children}: RoleGuardProps) {
    const {user} = useAuth();

    if (!user || !user.roles.some((role) => allowedRoles.includes(role))) {
        return (
            <div className="page-stack">
                <section className="table-card">
                    <div className="detail-block">
                        <p>Access restricted for this account role.</p>
                    </div>
                </section>
            </div>
        );
    }

    return <>{children}</>;
}

