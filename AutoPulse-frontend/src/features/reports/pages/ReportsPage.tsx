import {PageHeader} from "@/components/PageHeader";
import {TableCard} from "@/components/TableCard";
import {Tag} from "@/components/Tag";
import {useOpsData} from "@/features/ops/OpsDataContext";

export function ReportsPage() {
    const {users, depots, couriers, vehicles, parcels, routes, deliveryRuns} = useOpsData();

    const systemHealthRows = [
        ["Users active", `${users.filter((user) => user.active).length}/${users.length}`],
        ["Depots active", `${depots.filter((depot) => depot.active).length}/${depots.length}`],
        ["Available couriers", `${couriers.filter((courier) => courier.availabilityStatus === "AVAILABLE").length}/${couriers.length}`],
        ["Vehicles available", `${vehicles.filter((vehicle) => vehicle.status === "AVAILABLE").length}/${vehicles.length}`],
        ["Parcels delivered", `${parcels.filter((parcel) => parcel.status === "DELIVERED").length}/${parcels.length}`],
        ["Routes published", `${routes.filter((route) => route.status === "PUBLISHED").length}/${routes.length}`],
        ["Runs completed", `${deliveryRuns.filter((run) => run.status === "COMPLETED").length}/${deliveryRuns.length}`]
    ];

    return (
        <div className="page-stack">
            <PageHeader
                title="Admin Console"
                subtitle="High-level operational summary for administrators."
                actions={<Tag text="Admin-only page" tone="success"/>}
            />

            <TableCard title="System Snapshot" caption="Quick KPI-like summary" columns={["Metric", "Value"]}
                       rows={systemHealthRows}/>
        </div>
    );
}

