import {createContext, type ReactNode, useCallback, useContext, useEffect, useState} from "react";
import {apiRequest} from "@/api/httpClient";
import {useAuth} from "@/features/auth/AuthContext";

export type UserRole = "ADMIN" | "DISPATCHER" | "COURIER";
export type CourierAvailability = "AVAILABLE" | "OFF_DUTY" | "SUSPENDED" | "ON_ROUTE";
export type VehicleStatus = "AVAILABLE" | "IN_USE" | "MAINTENANCE" | "OUT_OF_SERVICE";
export type ParcelStatus =
    | "CREATED"
    | "PICKED_UP_BY_SENDER_REGIONAL_COURIER"
    | "IN_SENDER_REGIONAL_DEPOSIT"
    | "IN_TRANSIT"
    | "IN_RECEIVER_REGIONAL_DEPOSIT"
    | "IN_DELIVERY"
    | "OUT_FOR_DELIVERY"
    | "WAITING_IN_DEPOT"
    | "REJECTED"
    | "DELIVERED"
    | "FAILED_DELIVERY"
    | "RETURNED"
    | "CANCELLED";
export type RouteType = "SENDER_TO_REGIONAL" | "REGIONAL_TO_REGIONAL" | "REGIONAL_TO_RECEIVER";
export type RouteStatus = "PLANNED" | "PUBLISHED";
export type DeliveryRunStatus = "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED" | "ABORTED";

export interface OpsUser {
    id: string;
    fullName: string;
    email: string;
    phoneNumber: string;
    role: UserRole;
    active: boolean;
    regionCode?: string;
}

export interface OpsCourier {
    id: string;
    userId: string;
    depotId: string;
    regionCode: string;
    availabilityStatus: CourierAvailability;
}

export interface OpsDispatcher {
    id: string;
    userId: string;
    regionCode: string;
}

export interface OpsDepot {
    id: string;
    code: string;
    name: string;
    regionCode: string;
    locality: string;
    address: string;
    contactName: string;
    contactPhone: string;
    active: boolean;
}

export interface OpsVehicle {
    id: string;
    depotId: string;
    licensePlate: string;
    model: string;
    category: string;
    status: VehicleStatus;
    assignedCourierId?: string;
}

export interface OpsParcel {
    id: string;
    awb: string;
    depotId: string;
    status: ParcelStatus;
    receiverName: string;
    weightKg: number;
    volume: number;
    paymentRequired: boolean;
    updatedAt: string;
}

export interface OpsRoute {
    id: string;
    depotId: string;
    routeDate: string;
    routeType: RouteType;
    status: RouteStatus;
    courierId?: string;
    vehicleId?: string;
    parcelIds: string[];
    stops: number;
}

export interface OpsDeliveryRun {
    id: string;
    routeId: string;
    courierId: string;
    vehicleId: string;
    routeDate: string;
    status: DeliveryRunStatus;
    totalParcelCount: number;
    completedParcelCount: number;
}

interface OpsDataContextValue {
    isBootstrapping: boolean;
    bootstrapError: string | null;
    users: OpsUser[];
    couriers: OpsCourier[];
    dispatchers: OpsDispatcher[];
    depots: OpsDepot[];
    vehicles: OpsVehicle[];
    parcels: OpsParcel[];
    routes: OpsRoute[];
    deliveryRuns: OpsDeliveryRun[];
    refreshDashboardData: () => void;
    setUserActive: (userId: string, active: boolean) => void;
    setCourierAvailability: (courierId: string, availabilityStatus: CourierAvailability) => void;
    setDepotActive: (depotId: string, active: boolean) => void;
    setVehicleStatus: (vehicleId: string, status: VehicleStatus) => void;
    assignVehicle: (vehicleId: string, courierId?: string) => void;
    setParcelStatus: (parcelId: string, status: ParcelStatus) => void;
    createParcel: (depotId?: string) => OpsParcel;
    assignRoute: (routeId: string, courierId: string, vehicleId: string) => void;
    setRouteStatus: (routeId: string, status: RouteStatus) => void;
    setDeliveryRunStatus: (runId: string, status: DeliveryRunStatus) => void;
}

interface PageResponse<T> {
    content?: T[];
}

interface UserResponseDto {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
    active: boolean;
}

interface CourierResponseDto {
    courierProfileId: string;
    depotId: string;
    regionCode: string;
    availabilityStatus: string;
    user: UserResponseDto;
}

interface DispatcherResponseDto {
    dispatcherProfileId: string;
    regionCode: string;
    user: UserResponseDto;
}

interface DepotResponseDto {
    id: string;
    name: string;
    addressStreet?: string;
    addressNumber?: string;
    county?: string;
    city?: string;
    regionId?: string;
    contactName?: string;
    contactPhone?: string;
    depotCode?: string;
    active?: boolean;
}

interface RegionResponseDto {
    id: string;
    code: string;
}

interface VehicleResponseDto {
    id: string;
    depotId: string;
    licensePlate: string;
    brand?: string;
    model?: string;
    category?: string;
    status?: string;
}

interface VehicleAssignmentResponseDto {
    id: string;
    vehicleId: string;
    courierId: string;
    status: string;
}

interface ParcelSummaryResponseDto {
    id: string;
    awb: string;
    depotCode: string;
    receiverName: string;
    weight?: number;
    volume?: number;
    status?: string;
    createdAt?: string;
}

interface ContactResponseDto {
    name: string;
}

interface ParcelResponseDto {
    id: string;
    awb: string;
    depotCode: string;
    receiverContact?: ContactResponseDto;
    weight?: number;
    volume?: number;
    status?: string;
    paymentRequired?: boolean;
    updatedAt?: string;
}

interface DeliveryRunDto {
    id: string;
    routeId: string;
    courierId: string;
    vehicleId: string;
    routeDate: string;
    totalParcelCount?: number;
    status?: string;
}

interface StoredVehicleAssignment {
    id: string;
    vehicleId: string;
    courierId: string;
    status: "ACTIVE" | "ENDED" | "CANCELLED";
}

const OpsDataContext = createContext<OpsDataContextValue | null>(null);

const initialUsers: OpsUser[] = [
    {
        id: "usr-admin-1",
        fullName: "Operations Admin",
        email: "admin@autopulse.local",
        phoneNumber: "+40740000001",
        role: "ADMIN",
        active: true,
        regionCode: "RO-B"
    },
    {
        id: "usr-disp-1",
        fullName: "Andrei Ionescu",
        email: "dispatcher@autopulse.local",
        phoneNumber: "+40740000002",
        role: "DISPATCHER",
        active: true,
        regionCode: "RO-B"
    },
    {
        id: "usr-disp-2",
        fullName: "Ana Marinescu",
        email: "dispatcher.south@autopulse.local",
        phoneNumber: "+40740000003",
        role: "DISPATCHER",
        active: true,
        regionCode: "RO-DJ"
    },
    {
        id: "usr-cour-198",
        fullName: "Courier 198",
        email: "courier.198@autopulse.local",
        phoneNumber: "+40750000198",
        role: "COURIER",
        active: true,
        regionCode: "RO-B"
    },
    {
        id: "usr-cour-087",
        fullName: "Courier 087",
        email: "courier.087@autopulse.local",
        phoneNumber: "+40750000087",
        role: "COURIER",
        active: true,
        regionCode: "RO-DJ"
    },
    {
        id: "usr-cour-221",
        fullName: "Courier 221",
        email: "courier.221@autopulse.local",
        phoneNumber: "+40750000221",
        role: "COURIER",
        active: true,
        regionCode: "RO-B"
    }
];

const initialDepots: OpsDepot[] = [
    {
        id: "dep-north",
        code: "DEP-N",
        name: "Depot North",
        regionCode: "RO-B",
        locality: "Bucharest",
        address: "Soseaua Nordului 10",
        contactName: "Mirela Stan",
        contactPhone: "+40741000111",
        active: true
    },
    {
        id: "dep-south",
        code: "DEP-S",
        name: "Depot South",
        regionCode: "RO-DJ",
        locality: "Craiova",
        address: "Str. Industriei 14",
        contactName: "Radu Pavel",
        contactPhone: "+40741000222",
        active: true
    },
    {
        id: "dep-east",
        code: "DEP-E",
        name: "Depot East",
        regionCode: "RO-CT",
        locality: "Constanta",
        address: "Bd. Portului 3",
        contactName: "Elena Rusu",
        contactPhone: "+40741000333",
        active: false
    }
];

const initialCouriers: OpsCourier[] = [
    {id: "cour-198", userId: "usr-cour-198", depotId: "dep-north", regionCode: "RO-B", availabilityStatus: "ON_ROUTE"},
    {
        id: "cour-087",
        userId: "usr-cour-087",
        depotId: "dep-south",
        regionCode: "RO-DJ",
        availabilityStatus: "AVAILABLE"
    },
    {id: "cour-221", userId: "usr-cour-221", depotId: "dep-north", regionCode: "RO-B", availabilityStatus: "AVAILABLE"}
];

const initialDispatchers: OpsDispatcher[] = [
    {id: "disp-1002", userId: "usr-disp-1", regionCode: "RO-B"},
    {id: "disp-1003", userId: "usr-disp-2", regionCode: "RO-DJ"}
];

const initialVehicles: OpsVehicle[] = [
    {
        id: "veh-223",
        depotId: "dep-north",
        licensePlate: "B-102-AP",
        model: "Ford Transit",
        category: "VAN",
        status: "IN_USE",
        assignedCourierId: "cour-198"
    },
    {
        id: "veh-101",
        depotId: "dep-south",
        licensePlate: "DJ-553-AP",
        model: "Iveco Daily",
        category: "TRUCK",
        status: "AVAILABLE"
    },
    {
        id: "veh-145",
        depotId: "dep-north",
        licensePlate: "B-299-AP",
        model: "Renault Master",
        category: "VAN",
        status: "MAINTENANCE"
    }
];

const initialParcels: OpsParcel[] = [
    {
        id: "par-100932",
        awb: "AP-100932",
        depotId: "dep-north",
        status: "OUT_FOR_DELIVERY",
        receiverName: "Ioan Popescu",
        weightKg: 2.1,
        volume: 0.015,
        paymentRequired: true,
        updatedAt: "2026-04-29T10:42:00Z"
    },
    {
        id: "par-100875",
        awb: "AP-100875",
        depotId: "dep-south",
        status: "FAILED_DELIVERY",
        receiverName: "Ana Dobre",
        weightKg: 4.8,
        volume: 0.03,
        paymentRequired: false,
        updatedAt: "2026-04-29T08:12:00Z"
    },
    {
        id: "par-100911",
        awb: "AP-100911",
        depotId: "dep-east",
        status: "IN_TRANSIT",
        receiverName: "Mihai Iancu",
        weightKg: 1.3,
        volume: 0.01,
        paymentRequired: false,
        updatedAt: "2026-04-29T09:05:00Z"
    },
    {
        id: "par-100955",
        awb: "AP-100955",
        depotId: "dep-north",
        status: "WAITING_IN_DEPOT",
        receiverName: "Paula Enache",
        weightKg: 3.2,
        volume: 0.022,
        paymentRequired: true,
        updatedAt: "2026-04-29T07:30:00Z"
    },
    {
        id: "par-100960",
        awb: "AP-100960",
        depotId: "dep-north",
        status: "IN_RECEIVER_REGIONAL_DEPOSIT",
        receiverName: "Cristian Voicu",
        weightKg: 2.7,
        volume: 0.018,
        paymentRequired: false,
        updatedAt: "2026-04-29T06:25:00Z"
    }
];

const initialRoutes: OpsRoute[] = [
    {
        id: "route-301",
        depotId: "dep-north",
        routeDate: "2026-04-29",
        routeType: "REGIONAL_TO_RECEIVER",
        status: "PUBLISHED",
        courierId: "cour-198",
        vehicleId: "veh-223",
        parcelIds: ["par-100932", "par-100955", "par-100960"],
        stops: 14
    },
    {
        id: "route-302",
        depotId: "dep-south",
        routeDate: "2026-04-29",
        routeType: "REGIONAL_TO_RECEIVER",
        status: "PLANNED",
        courierId: "cour-087",
        vehicleId: "veh-101",
        parcelIds: ["par-100875"],
        stops: 9
    },
    {
        id: "route-303",
        depotId: "dep-north",
        routeDate: "2026-04-30",
        routeType: "SENDER_TO_REGIONAL",
        status: "PLANNED",
        parcelIds: [],
        stops: 7
    }
];

const initialDeliveryRuns: OpsDeliveryRun[] = [
    {
        id: "run-1110",
        routeId: "route-301",
        courierId: "cour-198",
        vehicleId: "veh-223",
        routeDate: "2026-04-29",
        status: "IN_PROGRESS",
        totalParcelCount: 3,
        completedParcelCount: 1
    },
    {
        id: "run-1111",
        routeId: "route-302",
        courierId: "cour-087",
        vehicleId: "veh-101",
        routeDate: "2026-04-29",
        status: "NOT_STARTED",
        totalParcelCount: 1,
        completedParcelCount: 0
    }
];

const courierAvailabilitySet = new Set<CourierAvailability>(["AVAILABLE", "OFF_DUTY", "SUSPENDED", "ON_ROUTE"]);
const vehicleStatusSet = new Set<VehicleStatus>(["AVAILABLE", "IN_USE", "MAINTENANCE", "OUT_OF_SERVICE"]);
const parcelStatusSet = new Set<ParcelStatus>([
    "CREATED",
    "PICKED_UP_BY_SENDER_REGIONAL_COURIER",
    "IN_SENDER_REGIONAL_DEPOSIT",
    "IN_TRANSIT",
    "IN_RECEIVER_REGIONAL_DEPOSIT",
    "IN_DELIVERY",
    "OUT_FOR_DELIVERY",
    "WAITING_IN_DEPOT",
    "REJECTED",
    "DELIVERED",
    "FAILED_DELIVERY",
    "RETURNED",
    "CANCELLED"
]);
const routeStatusSet = new Set<RouteStatus>(["PLANNED", "PUBLISHED"]);
const deliveryRunStatusSet = new Set<DeliveryRunStatus>(["NOT_STARTED", "IN_PROGRESS", "COMPLETED", "ABORTED"]);

function isUuid(value: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}

function withPagination(path: string): string {
    return path.includes("?") ? `${path}&page=0&size=200` : `${path}?page=0&size=200`;
}

async function fetchPagedContent<T>(path: string): Promise<T[]> {
    const response = await apiRequest<PageResponse<T>>(withPagination(path));
    if (!Array.isArray(response.content)) {
        return [];
    }
    return response.content;
}

function normalizePagedOrArray<T>(response: PageResponse<T> | T[]): T[] {
    if (Array.isArray(response)) {
        return response;
    }
    return Array.isArray(response.content) ? response.content : [];
}

function normalizeCourierAvailability(value: string | undefined): CourierAvailability {
    const candidate = value as CourierAvailability | undefined;
    if (candidate && courierAvailabilitySet.has(candidate)) {
        return candidate;
    }
    return "OFF_DUTY";
}

function normalizeVehicleStatus(value: string | undefined): VehicleStatus {
    const candidate = value as VehicleStatus | undefined;
    if (candidate && vehicleStatusSet.has(candidate)) {
        return candidate;
    }
    return "AVAILABLE";
}

function normalizeParcelStatus(value: string | undefined): ParcelStatus {
    const candidate = value as ParcelStatus | undefined;
    if (candidate && parcelStatusSet.has(candidate)) {
        return candidate;
    }
    return "CREATED";
}

function toBackendParcelStatus(status: ParcelStatus): string {
    if (status === "OUT_FOR_DELIVERY") {
        return "IN_DELIVERY";
    }
    if (status === "FAILED_DELIVERY" || status === "RETURNED" || status === "CANCELLED" || status === "REJECTED") {
        return "WAITING_IN_DEPOT";
    }
    return status;
}

function normalizeRouteStatus(value: string | undefined): RouteStatus {
    const candidate = value as RouteStatus | undefined;
    if (candidate && routeStatusSet.has(candidate)) {
        return candidate;
    }
    return "PLANNED";
}

function normalizeDeliveryRunStatus(value: string | undefined): DeliveryRunStatus {
    const candidate = value as DeliveryRunStatus | undefined;
    if (candidate && deliveryRunStatusSet.has(candidate)) {
        return candidate;
    }
    return "NOT_STARTED";
}

function formatDateForApi(date: Date): string {
    return date.toISOString().slice(0, 10);
}

function combineName(firstName: string | undefined, lastName: string | undefined): string {
    const fullName = `${firstName ?? ""} ${lastName ?? ""}`.trim();
    return fullName.length > 0 ? fullName : "Unnamed User";
}

function splitFullName(fullName: string): { firstName: string; lastName: string } {
    const parts = fullName.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) {
        return {firstName: "AutoPulse", lastName: "User"};
    }
    if (parts.length === 1) {
        return {firstName: parts[0], lastName: "User"};
    }
    return {firstName: parts[0], lastName: parts.slice(1).join(" ")};
}

function buildSyntheticRoutes(
    depots: OpsDepot[],
    parcels: OpsParcel[],
    couriers: OpsCourier[],
    vehicles: OpsVehicle[]
): OpsRoute[] {
    if (depots.length === 0) {
        return [];
    }

    const today = formatDateForApi(new Date());

    return depots.map((depot, index) => {
        const depotParcels = parcels.filter((parcel) => parcel.depotId === depot.id).slice(0, 24);
        const depotCouriers = couriers.filter((courier) => courier.depotId === depot.id);
        const depotVehicles = vehicles.filter((vehicle) => vehicle.depotId === depot.id);
        const courier = depotCouriers.find((entry) => entry.availabilityStatus === "ON_ROUTE") ?? depotCouriers[0];
        const vehicle = depotVehicles.find((entry) => entry.assignedCourierId === courier?.id) ?? depotVehicles[0];

        return {
            id: `route-${index + 1}-${depot.id.slice(0, 8)}`,
            depotId: depot.id,
            routeDate: today,
            routeType: "REGIONAL_TO_RECEIVER",
            status: courier && vehicle ? "PUBLISHED" : "PLANNED",
            courierId: courier?.id,
            vehicleId: vehicle?.id,
            parcelIds: depotParcels.map((parcel) => parcel.id),
            stops: Math.max(1, depotParcels.length)
        };
    });
}

function buildSyntheticRuns(routes: OpsRoute[]): OpsDeliveryRun[] {
    const runs = routes
        .filter((route) => route.courierId && route.vehicleId)
        .map((route, index) => ({
            id: `run-${index + 1}-${route.id}`,
            routeId: route.id,
            courierId: route.courierId as string,
            vehicleId: route.vehicleId as string,
            routeDate: route.routeDate,
            status: "NOT_STARTED" as DeliveryRunStatus,
            totalParcelCount: route.parcelIds.length,
            completedParcelCount: 0
        }));

    return runs;
}

function mapDeliveryRunToRoute(run: OpsDeliveryRun, couriers: OpsCourier[], fallbackDepotId: string): OpsRoute {
    const courier = couriers.find((entry) => entry.id === run.courierId);
    return {
        id: run.routeId,
        depotId: courier?.depotId ?? fallbackDepotId,
        routeDate: run.routeDate,
        routeType: "REGIONAL_TO_RECEIVER",
        status: run.status === "NOT_STARTED" ? "PLANNED" : "PUBLISHED",
        courierId: run.courierId,
        vehicleId: run.vehicleId,
        parcelIds: [],
        stops: Math.max(1, run.totalParcelCount)
    };
}

function mapBackendDeliveryRun(run: DeliveryRunDto): OpsDeliveryRun {
    const status = normalizeDeliveryRunStatus(run.status);
    const totalParcelCount = run.totalParcelCount ?? 0;
    return {
        id: String(run.id),
        routeId: String(run.routeId),
        courierId: String(run.courierId),
        vehicleId: String(run.vehicleId),
        routeDate: run.routeDate,
        status,
        totalParcelCount,
        completedParcelCount: status === "COMPLETED" ? totalParcelCount : 0
    };
}

function mapUserFromDto(dto: UserResponseDto, role: UserRole, regionCode?: string): OpsUser {
    return {
        id: String(dto.id),
        fullName: combineName(dto.firstName, dto.lastName),
        email: dto.email,
        phoneNumber: dto.phoneNumber,
        role,
        active: Boolean(dto.active),
        regionCode
    };
}

function mapVehicleAssignment(dto: VehicleAssignmentResponseDto): StoredVehicleAssignment {
    const status = dto.status === "ACTIVE" || dto.status === "ENDED" || dto.status === "CANCELLED" ? dto.status : "ENDED";
    return {
        id: String(dto.id),
        vehicleId: String(dto.vehicleId),
        courierId: String(dto.courierId),
        status
    };
}

export function OpsDataProvider({children}: { children: ReactNode }) {
    const {isAuthenticated, user} = useAuth();

    const [isBootstrapping, setIsBootstrapping] = useState(false);
    const [bootstrapError, setBootstrapError] = useState<string | null>(null);
    const [users, setUsers] = useState(initialUsers);
    const [couriers, setCouriers] = useState(initialCouriers);
    const [dispatchers, setDispatchers] = useState(initialDispatchers);
    const [depots, setDepots] = useState(initialDepots);
    const [vehicles, setVehicles] = useState(initialVehicles);
    const [parcels, setParcels] = useState(initialParcels);
    const [routes, setRoutes] = useState(initialRoutes);
    const [deliveryRuns, setDeliveryRuns] = useState(initialDeliveryRuns);
    const [vehicleAssignments, setVehicleAssignments] = useState<StoredVehicleAssignment[]>([]);
    const [bootstrapRefreshKey, setBootstrapRefreshKey] = useState(0);

    const refreshDashboardData = useCallback(() => {
        if (!isAuthenticated) {
            return;
        }

        setBootstrapRefreshKey((current) => current + 1);
    }, [isAuthenticated]);

    useEffect(() => {
        if (!isAuthenticated) {
            setIsBootstrapping(false);
            setBootstrapError(null);
            setUsers(initialUsers);
            setCouriers(initialCouriers);
            setDispatchers(initialDispatchers);
            setDepots(initialDepots);
            setVehicles(initialVehicles);
            setParcels(initialParcels);
            setRoutes(initialRoutes);
            setDeliveryRuns(initialDeliveryRuns);
            setVehicleAssignments([]);
            return;
        }

        let isActive = true;
        setIsBootstrapping(true);
        setBootstrapError(null);

        void (async () => {
            try {
                const loadPage = async <T, >(path: string): Promise<T[]> => {
                    try {
                        return await fetchPagedContent<T>(path);
                    } catch {
                        return [];
                    }
                };

                const [
                    userRows,
                    courierRows,
                    dispatcherRows,
                    depotRows,
                    regionRows,
                    vehicleRows,
                    assignmentRows,
                    parcelRows
                ] = await Promise.all([
                    loadPage<UserResponseDto>("/api/users/query/users"),
                    loadPage<CourierResponseDto>("/api/users/query/couriers"),
                    loadPage<DispatcherResponseDto>("/api/users/query/dispatchers"),
                    loadPage<DepotResponseDto>("/api/fleet/depots"),
                    loadPage<RegionResponseDto>("/api/geography/query/regions"),
                    loadPage<VehicleResponseDto>("/api/fleet/vehicles"),
                    loadPage<VehicleAssignmentResponseDto>("/api/fleet/vehicle-assignments"),
                    loadPage<ParcelSummaryResponseDto>("/api/parcels")
                ]);

                if (!isActive) {
                    return;
                }

                const regionCodeById = new Map(regionRows.map((entry) => [entry.id, entry.code]));
                const mappedDepots =
                    depotRows.map((dto) => {
                        const address = [dto.addressStreet, dto.addressNumber].filter(Boolean).join(" ").trim();
                        const locality = [dto.city, dto.county].filter(Boolean).join(", ").trim();

                        return {
                            id: String(dto.id),
                            code: dto.depotCode ?? `DEP-${String(dto.id).slice(0, 6).toUpperCase()}`,
                            name: dto.name,
                            regionCode: dto.regionId ? regionCodeById.get(String(dto.regionId)) ?? "N/A" : "N/A",
                            locality: locality.length > 0 ? locality : "N/A",
                            address: address.length > 0 ? address : "-",
                            contactName: dto.contactName ?? "-",
                            contactPhone: dto.contactPhone ?? "-",
                            active: Boolean(dto.active)
                        } satisfies OpsDepot;
                    });

                const depotCodeToId = new Map(mappedDepots.map((entry) => [entry.code, entry.id]));
                const fallbackDepotId = mappedDepots[0]?.id ?? "unassigned-depot";

                const mappedCouriers =
                    courierRows.map((dto) => ({
                        id: String(dto.courierProfileId),
                        userId: String(dto.user.id),
                        depotId: String(dto.depotId),
                        regionCode: dto.regionCode,
                        availabilityStatus: normalizeCourierAvailability(dto.availabilityStatus)
                    }));

                const mappedDispatchers =
                    dispatcherRows.map((dto) => ({
                        id: String(dto.dispatcherProfileId),
                        userId: String(dto.user.id),
                        regionCode: dto.regionCode
                    }));

                const mappedAssignments = assignmentRows.map(mapVehicleAssignment);
                const activeAssignments = mappedAssignments.filter((entry) => entry.status === "ACTIVE");
                const assignmentByVehicleId = new Map(activeAssignments.map((entry) => [entry.vehicleId, entry]));

                const mappedVehicles =
                    vehicleRows.map((dto) => {
                        const assignedCourierId = assignmentByVehicleId.get(String(dto.id))?.courierId;
                        const model = [dto.brand, dto.model].filter(Boolean).join(" ").trim();
                        return {
                            id: String(dto.id),
                            depotId: String(dto.depotId),
                            licensePlate: dto.licensePlate,
                            model: model.length > 0 ? model : dto.model ?? "Unknown",
                            category: dto.category ?? "M1",
                            status: normalizeVehicleStatus(dto.status),
                            assignedCourierId
                        } satisfies OpsVehicle;
                    });

                const mappedParcels =
                    parcelRows.map((dto) => ({
                        id: String(dto.id),
                        awb: dto.awb,
                        depotId: depotCodeToId.get(dto.depotCode) ?? fallbackDepotId,
                        status: normalizeParcelStatus(dto.status),
                        receiverName: dto.receiverName,
                        weightKg: dto.weight ?? 0,
                        volume: dto.volume ?? 0,
                        paymentRequired: false,
                        updatedAt: dto.createdAt ?? new Date().toISOString()
                    }));

                const userMap = new Map<string, OpsUser>();
                for (const userDto of userRows) {
                    const mappedUser = mapUserFromDto(userDto, "ADMIN");
                    userMap.set(mappedUser.id, mappedUser);
                }
                for (const dispatcherDto of dispatcherRows) {
                    const mappedDispatcherUser = mapUserFromDto(
                        dispatcherDto.user,
                        "DISPATCHER",
                        dispatcherDto.regionCode
                    );
                    userMap.set(mappedDispatcherUser.id, mappedDispatcherUser);
                }
                for (const courierDto of courierRows) {
                    const mappedCourierUser = mapUserFromDto(courierDto.user, "COURIER", courierDto.regionCode);
                    userMap.set(mappedCourierUser.id, mappedCourierUser);
                }
                const mappedUsers = Array.from(userMap.values());

                const syntheticRoutes = buildSyntheticRoutes(mappedDepots, mappedParcels, mappedCouriers, mappedVehicles);

                let backendRuns: OpsDeliveryRun[] = [];
                const couriersForRunSync = mappedCouriers.filter((entry) => isUuid(entry.id)).slice(0, 10);
                if (couriersForRunSync.length > 0) {
                    const routeDate = formatDateForApi(new Date());
                    const runLists = await Promise.all(
                        couriersForRunSync.map((entry) =>
                            apiRequest<PageResponse<DeliveryRunDto> | DeliveryRunDto[]>(
                                `/api/delivery-execution/runs?courierId=${entry.id}&routeDate=${routeDate}`
                            ).catch(() => [])
                        )
                    );
                    if (!isActive) {
                        return;
                    }
                    const runMap = new Map<string, OpsDeliveryRun>();
                    for (const response of runLists) {
                        const list = normalizePagedOrArray(response);
                        for (const run of list) {
                            const mappedRun = mapBackendDeliveryRun(run);
                            runMap.set(mappedRun.id, mappedRun);
                        }
                    }
                    backendRuns = Array.from(runMap.values());
                }

                const routeMap = new Map<string, OpsRoute>();
                for (const route of syntheticRoutes) {
                    routeMap.set(route.id, route);
                }
                for (const run of backendRuns) {
                    routeMap.set(run.routeId, mapDeliveryRunToRoute(run, mappedCouriers, fallbackDepotId));
                }
                const finalRoutes = Array.from(routeMap.values());
                const finalRuns = backendRuns.length > 0 ? backendRuns : buildSyntheticRuns(finalRoutes);

                setUsers(mappedUsers);
                setCouriers(mappedCouriers);
                setDispatchers(mappedDispatchers);
                setDepots(mappedDepots);
                setVehicles(mappedVehicles);
                setParcels(mappedParcels);
                setRoutes(finalRoutes);
                setDeliveryRuns(finalRuns);
                setVehicleAssignments(mappedAssignments);
            } catch (error) {
                if (isActive) {
                    setBootstrapError(error instanceof Error ? error.message : "Could not load operational data.");
                }
            } finally {
                if (isActive) {
                    setIsBootstrapping(false);
                }
            }
        })();

        return () => {
            isActive = false;
        };
    }, [bootstrapRefreshKey, isAuthenticated]);

    function syncVehicleAssignment(vehicleId: string, courierId?: string) {
        if (!isUuid(vehicleId)) {
            return;
        }

        void (async () => {
            const activeByVehicle = vehicleAssignments.find(
                (entry) => entry.vehicleId === vehicleId && entry.status === "ACTIVE"
            );
            const activeByCourier = courierId
                ? vehicleAssignments.find(
                    (entry) => entry.courierId === courierId && entry.status === "ACTIVE"
                )
                : undefined;
            const endedIds = new Set<string>();

            const tryEndAssignment = async (assignment: StoredVehicleAssignment | undefined) => {
                if (!assignment || !isUuid(assignment.id)) {
                    return;
                }
                try {
                    await apiRequest<VehicleAssignmentResponseDto>(
                        `/api/fleet/vehicle-assignments/${assignment.id}/end`,
                        {method: "PATCH"}
                    );
                    endedIds.add(assignment.id);
                } catch {
                    // Keep local fallback assignment state when endpoint is unavailable.
                }
            };

            await tryEndAssignment(activeByVehicle);
            if (activeByCourier && activeByCourier.id !== activeByVehicle?.id) {
                await tryEndAssignment(activeByCourier);
            }

            if (endedIds.size > 0) {
                setVehicleAssignments((current) =>
                    current.map((entry) =>
                        endedIds.has(entry.id) ? {...entry, status: "ENDED"} : entry
                    )
                );
            }

            if (!courierId || !isUuid(courierId)) {
                return;
            }

            try {
                const created = await apiRequest<VehicleAssignmentResponseDto>("/api/fleet/vehicle-assignments", {
                    method: "POST",
                    body: {vehicleId, courierId}
                });
                const mappedCreated = mapVehicleAssignment(created);
                setVehicleAssignments((current) => {
                    const withoutExisting = current.filter((entry) => entry.id !== mappedCreated.id);
                    return [...withoutExisting, mappedCreated];
                });
            } catch {
                // Keep local fallback assignment state when endpoint is unavailable.
            }
        })();
    }

    const value: OpsDataContextValue = {
        isBootstrapping,
        bootstrapError,
        users,
        couriers,
        dispatchers,
        depots,
        vehicles,
        parcels,
        routes,
        deliveryRuns,
        refreshDashboardData,
        setUserActive(userId, active) {
            setUsers((current) =>
                current.map((entry) =>
                    entry.id === userId
                        ? {
                            ...entry,
                            active
                        }
                        : entry
                )
            );

            if (!isUuid(userId)) {
                return;
            }

            const endpoint = active ? "activate" : "deactivate";
            void apiRequest<UserResponseDto>(`/api/users/admin/${userId}/${endpoint}`, {
                method: "PATCH"
            })
                .then((updatedUser) => {
                    setUsers((current) =>
                        current.map((entry) =>
                            entry.id === userId
                                ? {
                                    ...entry,
                                    fullName: combineName(updatedUser.firstName, updatedUser.lastName),
                                    email: updatedUser.email,
                                    phoneNumber: updatedUser.phoneNumber,
                                    active: updatedUser.active
                                }
                                : entry
                        )
                    );
                })
                .catch(() => undefined);
        },
        setCourierAvailability(courierId, availabilityStatus) {
            setCouriers((current) =>
                current.map((entry) =>
                    entry.id === courierId
                        ? {
                            ...entry,
                            availabilityStatus
                        }
                        : entry
                )
            );

            const courier = couriers.find((entry) => entry.id === courierId);
            const courierUser = users.find((entry) => entry.id === courier?.userId);

            if (!courier || !courierUser || !isUuid(courierId) || !isUuid(courier.depotId)) {
                return;
            }

            const updatePath =
                user?.role === "ADMIN"
                    ? `/api/users/admin/couriers/${courierId}`
                    : `/api/users/dispatcher/couriers/${courierId}`;
            const name = splitFullName(courierUser.fullName);

            void apiRequest<CourierResponseDto>(updatePath, {
                method: "PATCH",
                body: {
                    email: courierUser.email,
                    firstName: name.firstName,
                    lastName: name.lastName,
                    phoneNumber: courierUser.phoneNumber,
                    depotId: courier.depotId,
                    regionCode: courier.regionCode,
                    availabilityStatus,
                    active: courierUser.active
                }
            })
                .then((updatedCourier) => {
                    const mappedAvailability = normalizeCourierAvailability(updatedCourier.availabilityStatus);
                    setCouriers((current) =>
                        current.map((entry) =>
                            entry.id === courierId
                                ? {
                                    ...entry,
                                    availabilityStatus: mappedAvailability,
                                    depotId: String(updatedCourier.depotId),
                                    regionCode: updatedCourier.regionCode
                                }
                                : entry
                        )
                    );
                    setUsers((current) =>
                        current.map((entry) =>
                            entry.id === courierUser.id
                                ? {
                                    ...entry,
                                    fullName: combineName(updatedCourier.user.firstName, updatedCourier.user.lastName),
                                    email: updatedCourier.user.email,
                                    phoneNumber: updatedCourier.user.phoneNumber,
                                    active: updatedCourier.user.active,
                                    regionCode: updatedCourier.regionCode
                                }
                                : entry
                        )
                    );
                })
                .catch(() => undefined);
        },
        setDepotActive(depotId, active) {
            setDepots((current) =>
                current.map((entry) =>
                    entry.id === depotId
                        ? {
                            ...entry,
                            active
                        }
                        : entry
                )
            );

            if (!isUuid(depotId)) {
                return;
            }

            const endpoint = active ? "activate" : "deactivate";
            void apiRequest<DepotResponseDto>(`/api/fleet/depots/${depotId}/${endpoint}`, {
                method: "PATCH"
            })
                .then((updatedDepot) => {
                    setDepots((current) =>
                        current.map((entry) =>
                            entry.id === depotId
                                ? {
                                    ...entry,
                                    name: updatedDepot.name,
                                    code: updatedDepot.depotCode ?? entry.code,
                                    active: Boolean(updatedDepot.active)
                                }
                                : entry
                        )
                    );
                })
                .catch(() => undefined);
        },
        setVehicleStatus(vehicleId, status) {
            setVehicles((current) =>
                current.map((entry) =>
                    entry.id === vehicleId
                        ? {
                            ...entry,
                            status
                        }
                        : entry
                )
            );

            if (!isUuid(vehicleId)) {
                return;
            }

            void apiRequest<VehicleResponseDto>(`/api/fleet/vehicles/${vehicleId}/status`, {
                method: "PATCH",
                body: {status}
            })
                .then((updatedVehicle) => {
                    setVehicles((current) =>
                        current.map((entry) =>
                            entry.id === vehicleId
                                ? {
                                    ...entry,
                                    licensePlate: updatedVehicle.licensePlate,
                                    model: [updatedVehicle.brand, updatedVehicle.model].filter(Boolean).join(" ").trim() || entry.model,
                                    category: updatedVehicle.category ?? entry.category,
                                    status: normalizeVehicleStatus(updatedVehicle.status)
                                }
                                : entry
                        )
                    );
                })
                .catch(() => undefined);
        },
        assignVehicle(vehicleId, courierId) {
            setVehicles((current) =>
                current.map((entry) => {
                    if (entry.id === vehicleId) {
                        return {
                            ...entry,
                            assignedCourierId: courierId
                        };
                    }
                    if (courierId && entry.assignedCourierId === courierId) {
                        return {
                            ...entry,
                            assignedCourierId: undefined
                        };
                    }
                    return entry;
                })
            );

            syncVehicleAssignment(vehicleId, courierId);
        },
        setParcelStatus(parcelId, status) {
            const nowIso = new Date().toISOString();
            setParcels((current) =>
                current.map((entry) =>
                    entry.id === parcelId
                        ? {
                            ...entry,
                            status,
                            updatedAt: nowIso
                        }
                        : entry
                )
            );

            if (!isUuid(parcelId)) {
                return;
            }

            void (async () => {
                try {
                    await apiRequest<void>(`/api/parcels/${parcelId}/status`, {
                        method: "POST",
                        body: {
                            status: toBackendParcelStatus(status)
                        }
                    });
                } catch {
                    return;
                }

                try {
                    const latest = await apiRequest<ParcelResponseDto>(`/api/parcels/${parcelId}`);
                    setParcels((current) =>
                        current.map((entry) => {
                            if (entry.id !== parcelId) {
                                return entry;
                            }

                            const mappedDepotId =
                                depots.find((depot) => depot.code === latest.depotCode)?.id ?? entry.depotId;
                            return {
                                ...entry,
                                awb: latest.awb,
                                depotId: mappedDepotId,
                                status: normalizeParcelStatus(latest.status),
                                receiverName: latest.receiverContact?.name ?? entry.receiverName,
                                weightKg: latest.weight ?? entry.weightKg,
                                volume: latest.volume ?? entry.volume,
                                paymentRequired: Boolean(latest.paymentRequired),
                                updatedAt: latest.updatedAt ?? nowIso
                            };
                        })
                    );
                } catch {
                    // Keep local fallback state when refresh endpoint is unavailable.
                }
            })();
        },
        createParcel(depotId) {
            const resolvedDepotId = depotId ?? depots[0]?.id ?? initialDepots[0].id;
            const timestamp = Date.now();
            const nowIso = new Date(timestamp).toISOString();
            const sequence = String(timestamp).slice(-8);
            const created: OpsParcel = {
                id: `local-parcel-${timestamp}`,
                awb: `AWB-${new Date(timestamp).toISOString().slice(0, 10).replaceAll("-", "")}-${sequence}`,
                depotId: resolvedDepotId,
                status: "CREATED",
                receiverName: "New Receiver",
                weightKg: 1,
                volume: 0.1,
                paymentRequired: false,
                updatedAt: nowIso
            };

            setParcels((current) => [created, ...current]);
            return created;
        },
        assignRoute(routeId, courierId, vehicleId) {
            setRoutes((current) =>
                current.map((entry) =>
                    entry.id === routeId
                        ? {
                            ...entry,
                            courierId,
                            vehicleId
                        }
                        : entry
                )
            );

            setVehicles((current) =>
                current.map((entry) => {
                    if (entry.id === vehicleId) {
                        return {
                            ...entry,
                            assignedCourierId: courierId
                        };
                    }
                    if (entry.assignedCourierId === courierId) {
                        return {
                            ...entry,
                            assignedCourierId: undefined
                        };
                    }
                    return entry;
                })
            );

            syncVehicleAssignment(vehicleId, courierId);
        },
        setRouteStatus(routeId, status) {
            const normalizedStatus = normalizeRouteStatus(status);
            setRoutes((current) =>
                current.map((entry) =>
                    entry.id === routeId
                        ? {
                            ...entry,
                            status: normalizedStatus
                        }
                        : entry
                )
            );
        },
        setDeliveryRunStatus(runId, status) {
            setDeliveryRuns((current) =>
                current.map((entry) =>
                    entry.id === runId
                        ? {
                            ...entry,
                            status,
                            completedParcelCount: status === "COMPLETED" ? entry.totalParcelCount : entry.completedParcelCount
                        }
                        : entry
                )
            );

            if (!isUuid(runId)) {
                return;
            }

            let endpoint: string | null = null;
            if (status === "IN_PROGRESS") {
                endpoint = "start";
            } else if (status === "ABORTED") {
                endpoint = "abort";
            } else if (status === "COMPLETED") {
                endpoint = "complete";
            }

            if (!endpoint) {
                return;
            }

            void apiRequest<DeliveryRunDto>(`/api/delivery-execution/runs/${runId}/${endpoint}`, {
                method: "POST"
            })
                .then((updatedRun) => {
                    const mappedRun = mapBackendDeliveryRun(updatedRun);
                    setDeliveryRuns((current) =>
                        current.map((entry) => (entry.id === runId ? mappedRun : entry))
                    );
                    setRoutes((current) =>
                        current.map((entry) =>
                            entry.id === mappedRun.routeId
                                ? {
                                    ...entry,
                                    status: mappedRun.status === "NOT_STARTED" ? "PLANNED" : "PUBLISHED"
                                }
                                : entry
                        )
                    );
                })
                .catch(() => undefined);
        }
    };

    return <OpsDataContext.Provider value={value}>{children}</OpsDataContext.Provider>;
}

export function useOpsData() {
    const context = useContext(OpsDataContext);

    if (!context) {
        throw new Error("useOpsData must be used within an OpsDataProvider");
    }

    return context;
}
