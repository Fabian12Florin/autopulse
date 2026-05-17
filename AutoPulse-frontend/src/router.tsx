import {createBrowserRouter, Navigate} from "react-router-dom";
import {useAuth} from "@/features/auth/AuthContext";
import {PortalShell} from "@/shell/PortalShell";
import {ProtectedRoute} from "@/shell/ProtectedRoute";
import {RoleGuard} from "@/shell/RoleGuard";
import {LoginPage} from "@/features/auth/pages/LoginPage";
import {PasswordResetPage} from "@/features/auth/pages/PasswordResetPage";
import {DashboardPage} from "@/features/dashboard/pages/DashboardPage";
import {UsersPage} from "@/features/users/pages/UsersPage";
import {CouriersPage} from "@/features/couriers/pages/CouriersPage";
import {DepotsPage} from "@/features/depots/pages/DepotsPage";
import {FleetPage} from "@/features/fleet/pages/FleetPage";
import {ParcelsPage} from "@/features/parcels/pages/ParcelsPage";
import {RoutesPage} from "@/features/routes/pages/RoutesPage";
import {DeliveryRunsPage} from "@/features/delivery-runs/pages/DeliveryRunsPage";
import {GeographyPage} from "@/features/geography/pages/GeographyPage";
import {ReportsPage} from "@/features/reports/pages/ReportsPage";
import {NotFoundPage} from "@/features/common/pages/NotFoundPage";

export const router = createBrowserRouter([
    {
        path: "/",
        element: (
            <ProtectedRoute>
                <PortalShell/>
            </ProtectedRoute>
        ),
        children: [
            {index: true, element: <HomeRedirect/>},
            {
                path: "dashboard",
                element: (
                    <RoleGuard allowedRoles={["ADMIN", "DISPATCHER"]}>
                        <DashboardPage/>
                    </RoleGuard>
                )
            },
            {
                path: "dispatchers",
                element: (
                    <RoleGuard allowedRoles={["ADMIN"]}>
                        <UsersPage/>
                    </RoleGuard>
                )
            },
            {path: "users", element: <Navigate to="/dispatchers" replace/>},
            {
                path: "couriers",
                element: (
                    <RoleGuard allowedRoles={["ADMIN", "DISPATCHER", "COURIER"]}>
                        <CouriersPage/>
                    </RoleGuard>
                )
            },
            {path: "depots", element: <DepotsPage/>},
            {path: "fleet", element: <FleetPage/>},
            {path: "parcels", element: <ParcelsPage/>},
            {
                path: "routes",
                element: (
                    <RoleGuard allowedRoles={["ADMIN", "DISPATCHER"]}>
                        <RoutesPage/>
                    </RoleGuard>
                )
            },
            {path: "route-planning", element: <Navigate to="/routes" replace/>},
            {
                path: "delivery-runs",
                element: (
                    <RoleGuard allowedRoles={["ADMIN", "DISPATCHER"]}>
                        <DeliveryRunsPage/>
                    </RoleGuard>
                )
            },
            {
                path: "geography",
                element: (
                    <RoleGuard allowedRoles={["ADMIN", "DISPATCHER"]}>
                        <GeographyPage/>
                    </RoleGuard>
                )
            },
            {
                path: "reports",
                element: (
                    <RoleGuard allowedRoles={["ADMIN"]}>
                        <ReportsPage/>
                    </RoleGuard>
                )
            }
        ]
    },
    {path: "/login", element: <LoginPage/>},
    {path: "/password-reset", element: <PasswordResetPage/>},
    {path: "*", element: <NotFoundPage/>}
]);

function HomeRedirect() {
    const {user} = useAuth();
    return <Navigate to={user?.roles.includes("COURIER") ? "couriers" : "dashboard"} replace/>;
}
