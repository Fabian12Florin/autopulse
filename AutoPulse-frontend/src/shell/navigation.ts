import type {PortalRole} from "@/features/auth/AuthContext";

export interface NavItem {
    label: string;
    path: string;
    description: string;
    roles: PortalRole[];
}

export const navItems: NavItem[] = [
    {label: "Dashboard", path: "/dashboard", description: "Operational overview", roles: ["ADMIN", "DISPATCHER"]},
    {label: "Dispatchers", path: "/dispatchers", description: "Profiles and lifecycle", roles: ["ADMIN"]},
    {
        label: "Couriers",
        path: "/couriers",
        description: "Availability and assignment",
        roles: ["ADMIN", "DISPATCHER", "COURIER"]
    },
    {label: "Parcels", path: "/parcels", description: "Search and lifecycle", roles: ["ADMIN", "DISPATCHER"]},
    {label: "Routes", path: "/routes", description: "Planning and selection", roles: ["ADMIN", "DISPATCHER"]},
    {
        label: "Delivery Runs",
        path: "/delivery-runs",
        description: "Live run monitoring",
        roles: ["ADMIN", "DISPATCHER"]
    },
    {label: "Depots", path: "/depots", description: "Depot details and load", roles: ["ADMIN", "DISPATCHER"]},
    {label: "Fleet", path: "/fleet", description: "Vehicles and assignments", roles: ["ADMIN", "DISPATCHER"]},
    {label: "Geography", path: "/geography", description: "Regions and counties", roles: ["ADMIN", "DISPATCHER"]},
    {label: "Admin Console", path: "/reports", description: "Platform summary", roles: ["ADMIN"]}
];
