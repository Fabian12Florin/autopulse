import {type ReactNode, useEffect} from "react";
import {PageHeader} from "@/components/PageHeader";
import {PageFeedback} from "@/components/PageFeedback";
import {StatCard} from "@/components/StatCard";
import {TableCard} from "@/components/TableCard";
import {Tag} from "@/components/Tag";
import {useNavigate} from "react-router-dom";
import {type SessionUser, useAuth} from "@/features/auth/AuthContext";
import {
    depotLabel as formatDepotLabel,
    routeLabel,
    runLabel,
    shortReference,
    vehicleLabel as formatVehicleLabel
} from "@/features/common/displayFormatters";
import {
    type OpsCourier,
    type OpsDeliveryRun,
    type OpsDepot,
    type OpsParcel,
    type OpsRoute,
    type OpsVehicle,
    useOpsData
} from "@/features/ops/OpsDataContext";

const dispatchStatuses = new Set(["CREATED", "IN_SENDER_REGIONAL_DEPOSIT", "IN_RECEIVER_REGIONAL_DEPOSIT", "WAITING_IN_DEPOT"]);
const exceptionStatuses = new Set(["FAILED_DELIVERY", "REJECTED", "RETURNED", "CANCELLED"]);

export function DashboardPage() {
    const {user} = useAuth();
    const navigate = useNavigate();
    const {
        isBootstrapping,
        bootstrapError,
        users,
        depots,
        couriers,
        dispatchers,
        parcels,
        routes,
        deliveryRuns,
        vehicles,
        refreshDashboardData
    } = useOpsData();

    useEffect(() => {
        refreshDashboardData();
    }, [refreshDashboardData]);
    const userById = new Map(users.map((entry) => [entry.id, entry]));
    const courierNameById = new Map(
        couriers.map((courier) => [courier.id, userById.get(courier.userId)?.fullName ?? `Courier ${shortReference(courier.id)}`])
    );
    const vehicleById = new Map(vehicles.map((vehicle) => [vehicle.id, vehicle]));

    const dispatcherProfile = dispatchers.find(
        (dispatcher) => dispatcher.id === user?.dispatcherProfileId || dispatcher.userId === user?.id
    );
    const regionCode = user?.regionCode ?? dispatcherProfile?.regionCode;
    const isAdmin = user?.roles.includes("ADMIN") ?? false;
    const scopeLabel = isAdmin ? "all regions" : regionCode ?? "assigned region";

    const scopedDepots = isAdmin
        ? depots
        : depots.filter((depot) => depot.id === user?.depotId || depot.regionCode === regionCode);
    const scopedDepotIds = new Set(scopedDepots.map((depot) => depot.id));
    const scopedCouriers = couriers.filter((courier) => isAdmin || isCourierInScope(courier, scopedDepotIds, regionCode));
    const scopedVehicles = vehicles.filter((vehicle) => isAdmin || scopedDepotIds.has(vehicle.depotId));
    const scopedParcels = parcels.filter((parcel) => isAdmin || scopedDepotIds.has(parcel.depotId));
    const scopedRoutes = routes.filter(
        (route) =>
            isAdmin ||
            scopedDepotIds.has(route.depotId) ||
            scopedCouriers.some((courier) => courier.id === route.courierId) ||
            scopedVehicles.some((vehicle) => vehicle.id === route.vehicleId)
    );
    const scopedRouteIds = new Set(scopedRoutes.map((route) => route.id));
    const scopedRuns = deliveryRuns.filter(
        (run) =>
            isAdmin ||
            scopedRouteIds.has(run.routeId) ||
            scopedCouriers.some((courier) => courier.id === run.courierId)
    );

    const availableCouriers = scopedCouriers.filter((courier) => courier.availabilityStatus === "AVAILABLE");
    const onRouteCouriers = scopedCouriers.filter((courier) => courier.availabilityStatus === "ON_ROUTE");
    const dispatchQueue = scopedParcels.filter((parcel) => dispatchStatuses.has(parcel.status));
    const exceptions = scopedParcels.filter((parcel) => exceptionStatuses.has(parcel.status));
    const activeRuns = scopedRuns.filter((run) => run.status === "IN_PROGRESS");
    const readyVehicles = scopedVehicles.filter((vehicle) => vehicle.status === "AVAILABLE");
    const blockedVehicles = scopedVehicles.filter(
        (vehicle) => vehicle.status === "MAINTENANCE" || vehicle.status === "OUT_OF_SERVICE"
    );

    return (
        <div className="page-stack">
            <PageHeader
                title={isAdmin ? "Admin Dashboard" : "Dispatcher Dashboard"}
                subtitle={
                    isAdmin
                        ? "Live operational view across users, depots, parcels, delivery runs, and fleet."
                        : `Operational queue for ${scopeLabel}.`
                }
                actions={<Tag
                    text={isBootstrapping ? "Loading backend data..." : isAdmin ? "ADMIN scope" : `DISPATCHER scope: ${scopeLabel}`}
                    tone="neutral"
                />}
            />
            <PageFeedback errors={bootstrapError ? [bootstrapError] : []}/>

            <section className="metrics-grid" style={isBootstrapping ? {position: "relative"} : undefined}>
                {isAdmin ? (
                    <>
                        <StatCard
                            label="Active depots"
                            value={`${scopedDepots.filter((depot) => depot.active).length}/${scopedDepots.length}`}
                            trend={`${depots.length} depots loaded from fleet-service`}
                            tone="neutral"
                            onClick={() => navigate("/depots")}
                        />
                        <StatCard
                            label="Couriers available"
                            value={`${availableCouriers.length}/${scopedCouriers.length}`}
                            trend={`${onRouteCouriers.length} currently on route`}
                            tone={availableCouriers.length > 0 ? "positive" : "warning"}
                            onClick={() => navigate("/couriers")}
                        />
                        <StatCard
                            label="Parcels to dispatch"
                            value={String(dispatchQueue.length)}
                            trend={`${exceptions.length} parcel exceptions`}
                            tone={exceptions.length > 0 ? "warning" : "positive"}
                            onClick={() => navigate("/parcels")}
                        />
                        <StatCard
                            label="Fleet blocked"
                            value={String(blockedVehicles.length)}
                            trend={`${readyVehicles.length} vehicles ready`}
                            tone={blockedVehicles.length > 0 ? "warning" : "positive"}
                            onClick={() => navigate("/fleet")}
                        />
                        <StatCard
                            label="Runs in progress"
                            value={String(activeRuns.length)}
                            trend={`${scopedRoutes.filter((route) => route.status === "PLANNED").length} planned routes`}
                            tone="neutral"
                            onClick={() => navigate("/delivery-runs")}
                        />
                    </>
                ) : (
                    <>
                        <StatCard
                            label="Depots in scope"
                            value={String(scopedDepots.length)}
                            trend={regionCode ? `Region ${regionCode}` : "No region claim found"}
                            tone={regionCode ? "neutral" : "warning"}
                            onClick={() => navigate("/depots")}
                        />
                        <StatCard
                            label="Available couriers"
                            value={`${availableCouriers.length}/${scopedCouriers.length}`}
                            trend={`${onRouteCouriers.length} on route`}
                            tone={availableCouriers.length > 0 ? "positive" : "warning"}
                            onClick={() => navigate("/couriers")}
                        />
                        <StatCard
                            label="Parcels waiting"
                            value={String(dispatchQueue.length)}
                            trend={`${exceptions.length} require attention`}
                            tone={exceptions.length > 0 ? "warning" : "neutral"}
                            onClick={() => navigate("/parcels")}
                        />
                        <StatCard
                            label="Active runs"
                            value={String(activeRuns.length)}
                            trend={`${scopedRuns.length} runs in scope`}
                            tone="neutral"
                            onClick={() => navigate("/delivery-runs")}
                        />
                        <StatCard
                            label="Vehicles ready"
                            value={`${readyVehicles.length}/${scopedVehicles.length}`}
                            trend={`${blockedVehicles.length} blocked`}
                            tone={readyVehicles.length > 0 ? "positive" : "warning"}
                            onClick={() => navigate("/fleet")}
                        />
                    </>
                )}
                {isBootstrapping ? <div className="loading-overlay" aria-hidden="true"/> : null}
            </section>

            {isAdmin ? (
                <AdminDashboardTables
                    depots={scopedDepots}
                    couriers={scopedCouriers}
                    parcels={scopedParcels}
                    vehicles={scopedVehicles}
                    deliveryRuns={scopedRuns}
                    routes={scopedRoutes}
                    exceptions={exceptions}
                    courierNameById={courierNameById}
                    loading={isBootstrapping}
                />
            ) : (
                <DispatcherDashboardTables
                    depots={scopedDepots}
                    couriers={scopedCouriers}
                    parcels={dispatchQueue}
                    vehicles={scopedVehicles}
                    deliveryRuns={scopedRuns}
                    routes={scopedRoutes}
                    exceptions={exceptions}
                    user={user}
                    courierNameById={courierNameById}
                    vehicleById={vehicleById}
                    loading={isBootstrapping}
                />
            )}
        </div>
    );
}

function AdminDashboardTables({
                                  depots,
                                  couriers,
                                  parcels,
                                  vehicles,
                                  deliveryRuns,
                                  routes,
                                  exceptions,
                                  courierNameById,
                                  loading
                              }: {
    depots: OpsDepot[];
    couriers: OpsCourier[];
    parcels: OpsParcel[];
    vehicles: OpsVehicle[];
    deliveryRuns: OpsDeliveryRun[];
    routes: OpsRoute[];
    exceptions: OpsParcel[];
    courierNameById: Map<string, string>;
    loading: boolean;
}) {
    return (
        <>
            <section className="two-column-grid">
                <TableCard
                    title="Depot Load"
                    caption="Fleet-service depots with operational load from parcel, user, and fleet APIs"
                    columns={["Depot", "Region", "Couriers", "Vehicles", "Parcels", "Status"]}
                    loading={loading}
                    rows={depots.map((depot) => [
                        depotDisplay(depot),
                        depot.regionCode,
                        String(couriers.filter((courier) => courier.depotId === depot.id).length),
                        String(vehicles.filter((vehicle) => vehicle.depotId === depot.id).length),
                        String(parcels.filter((parcel) => parcel.depotId === depot.id).length),
                        <Tag key={`${depot.id}-status`} text={depot.active ? "ACTIVE" : "INACTIVE"}
                             tone={depot.active ? "success" : "warning"}/>
                    ])}
                    footer={<EmptyHint visible={depots.length === 0}>No depots returned by fleet-service.</EmptyHint>}
                />

                <TableCard
                    title="Fleet Availability"
                    caption="Vehicle capacity signals by depot"
                    columns={["Depot", "Ready", "In use", "Blocked", "Assigned"]}
                    loading={loading}
                    rows={depots.map((depot) => {
                        const depotVehicles = vehicles.filter((vehicle) => vehicle.depotId === depot.id);
                        return [
                            depotDisplay(depot),
                            String(depotVehicles.filter((vehicle) => vehicle.status === "AVAILABLE").length),
                            String(depotVehicles.filter((vehicle) => vehicle.status === "IN_USE").length),
                            String(depotVehicles.filter((vehicle) => vehicle.status === "MAINTENANCE" || vehicle.status === "OUT_OF_SERVICE").length),
                            String(depotVehicles.filter((vehicle) => vehicle.assignedCourierId).length)
                        ];
                    })}
                    footer={<EmptyHint visible={vehicles.length === 0}>No vehicles returned by
                        fleet-service.</EmptyHint>}
                />
            </section>

            <section className="two-column-grid">
                <TableCard
                    title="Parcel Exceptions"
                    caption="Statuses that need admin attention across the network"
                    columns={["AWB", "Depot", "Receiver", "Status", "Updated"]}
                    loading={loading}
                    rows={exceptions.slice(0, 8).map((parcel) => [
                        parcel.awb,
                        depotName(parcel.depotId, depots),
                        parcel.receiverName,
                        <StatusTag key={`${parcel.id}-status`} value={parcel.status}/>,
                        formatDateTime(parcel.updatedAt)
                    ])}
                    footer={<EmptyHint visible={exceptions.length === 0}>No parcel exceptions in the loaded
                        data.</EmptyHint>}
                />

                <TableCard
                    title="Delivery Runs"
                    caption="Execution-service runs matched to planned routes"
                    columns={["Run", "Route", "Courier", "Status", "Progress"]}
                    loading={loading}
                    rows={deliveryRuns.slice(0, 8).map((run) => [
                        runLabel(run.id),
                        routeLabel(run.routeId),
                        courierDisplayName(run.courierId, courierNameById),
                        <StatusTag key={`${run.id}-status`} value={run.status}/>,
                        `${run.completedParcelCount}/${run.totalParcelCount}`
                    ])}
                    footer={<EmptyHint visible={deliveryRuns.length === 0}>No delivery runs returned for active couriers
                        today.</EmptyHint>}
                    headerAction={<Tag text={`${routes.length} routes`} tone="neutral"/>}
                />
            </section>
        </>
    );
}

function DispatcherDashboardTables({
                                       depots,
                                       couriers,
                                       parcels,
                                       vehicles,
                                       deliveryRuns,
                                       routes,
                                       exceptions,
                                       user,
                                       courierNameById,
                                       vehicleById,
                                       loading
                                   }: {
    depots: OpsDepot[];
    couriers: OpsCourier[];
    parcels: OpsParcel[];
    vehicles: OpsVehicle[];
    deliveryRuns: OpsDeliveryRun[];
    routes: OpsRoute[];
    exceptions: OpsParcel[];
    user: SessionUser | null;
    courierNameById: Map<string, string>;
    vehicleById: Map<string, OpsVehicle>;
    loading: boolean;
}) {
    return (
        <>
            <section className="two-column-grid">
                <TableCard
                    title="Available Couriers"
                    caption="Couriers the dispatcher can assign now"
                    columns={["Courier", "Depot", "Region", "Vehicle"]}
                    loading={loading}
                    rows={couriers
                        .filter((courier) => courier.availabilityStatus === "AVAILABLE")
                        .slice(0, 8)
                        .map((courier) => [
                            courierDisplayName(courier.id, courierNameById),
                            depotName(courier.depotId, depots),
                            courier.regionCode,
                            vehicles.find((vehicle) => vehicle.assignedCourierId === courier.id)?.licensePlate ?? "Unassigned"
                        ])}
                    footer={<EmptyHint
                        visible={couriers.filter((courier) => courier.availabilityStatus === "AVAILABLE").length === 0}>No
                        available couriers in this scope.</EmptyHint>}
                />

                <TableCard
                    title="Dispatch Queue"
                    caption="Parcels waiting for local dispatch decisions"
                    columns={["AWB", "Depot", "Receiver", "Status", "Updated"]}
                    loading={loading}
                    rows={parcels.slice(0, 8).map((parcel) => [
                        parcel.awb,
                        depotName(parcel.depotId, depots),
                        parcel.receiverName,
                        <StatusTag key={`${parcel.id}-status`} value={parcel.status}/>,
                        formatDateTime(parcel.updatedAt)
                    ])}
                    footer={<EmptyHint visible={parcels.length === 0}>No waiting parcels
                        for {user?.fullName ?? "this dispatcher"}.</EmptyHint>}
                />
            </section>

            <section className="two-column-grid">
                <TableCard
                    title="Runs In Scope"
                    caption="Execution status for the assigned region"
                    columns={["Run", "Route", "Vehicle", "Status", "Progress"]}
                    loading={loading}
                    rows={deliveryRuns.slice(0, 8).map((run) => [
                        runLabel(run.id),
                        routeLabel(run.routeId),
                        formatVehicleLabel(vehicleById.get(run.vehicleId), run.vehicleId),
                        <StatusTag key={`${run.id}-status`} value={run.status}/>,
                        `${run.completedParcelCount}/${run.totalParcelCount}`
                    ])}
                    footer={<EmptyHint visible={deliveryRuns.length === 0}>No delivery runs returned for this dispatcher
                        scope.</EmptyHint>}
                    headerAction={<Tag text={`${routes.length} routes`} tone="neutral"/>}
                />

                <TableCard
                    title="Attention Needed"
                    caption="Blocked fleet and parcel exceptions"
                    columns={["Type", "Reference", "Depot", "State"]}
                    loading={loading}
                    rows={[
                        ...vehicles
                            .filter((vehicle) => vehicle.status === "MAINTENANCE" || vehicle.status === "OUT_OF_SERVICE")
                            .map((vehicle) => [
                                "Vehicle",
                                vehicle.licensePlate,
                                depotName(vehicle.depotId, depots),
                                <StatusTag key={`${vehicle.id}-status`} value={vehicle.status}/>
                            ]),
                        ...exceptions.map((parcel) => [
                            "Parcel",
                            parcel.awb,
                            depotName(parcel.depotId, depots),
                            <StatusTag key={`${parcel.id}-status`} value={parcel.status}/>
                        ])
                    ].slice(0, 8)}
                    footer={<EmptyHint
                        visible={exceptions.length === 0 && vehicles.every((vehicle) => vehicle.status !== "MAINTENANCE" && vehicle.status !== "OUT_OF_SERVICE")}>No
                        blocked fleet or parcel exceptions.</EmptyHint>}
                />
            </section>
        </>
    );
}

function isCourierInScope(courier: OpsCourier, depotIds: Set<string>, regionCode?: string) {
    return depotIds.has(courier.depotId) || Boolean(regionCode && courier.regionCode === regionCode);
}

function depotDisplay(depot: OpsDepot) {
    return formatDepotLabel({id: depot.id, code: depot.code, name: depot.name});
}

function depotName(depotId: string, depots: OpsDepot[]) {
    const depot = depots.find((entry) => entry.id === depotId);
    return depot ? depotDisplay(depot) : `Depot ${shortReference(depotId)}`;
}

function courierDisplayName(courierId: string, courierNameById: Map<string, string>) {
    const name = courierNameById.get(courierId)?.trim();
    return name || `Courier ${shortReference(courierId)}`;
}

function StatusTag({value}: { value: string }) {
    return <Tag text={value} tone={statusTone(value)}/>;
}

function EmptyHint({visible, children}: { visible: boolean; children: ReactNode }) {
    return visible ? <span className="table-empty-hint">{children}</span> : null;
}

function statusTone(value: string) {
    if (value === "AVAILABLE" || value === "ACTIVE" || value === "IN_PROGRESS" || value === "DELIVERED") {
        return "success";
    }
    if (
        value === "MAINTENANCE" ||
        value === "OUT_OF_SERVICE" ||
        value === "FAILED_DELIVERY" ||
        value === "REJECTED" ||
        value === "RETURNED" ||
        value === "CANCELLED" ||
        value === "ABORTED"
    ) {
        return "danger";
    }
    if (value === "WAITING_IN_DEPOT" || value === "IN_RECEIVER_REGIONAL_DEPOSIT" || value === "PLANNED") {
        return "warning";
    }
    return "neutral";
}

function formatDateTime(value: string) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return "-";
    }
    return new Intl.DateTimeFormat("ro-RO", {
        day: "2-digit",
        month: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    }).format(date);
}
