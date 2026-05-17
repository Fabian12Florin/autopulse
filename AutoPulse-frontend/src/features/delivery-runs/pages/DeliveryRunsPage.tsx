import {type FormEvent, useEffect, useMemo, useState} from "react";
import {useAuth} from "@/features/auth/AuthContext";
import {PageFeedback} from "@/components/PageFeedback";
import {PageHeader} from "@/components/PageHeader";
import {TableCard} from "@/components/TableCard";
import {Tag} from "@/components/Tag";
import {PaginationControls} from "@/components/PaginationControls";
import {
    abortDeliveryRun,
    arriveAtDeliveryStop,
    completeDeliveryParcel,
    completeDeliveryRun,
    type DeliveryOutcome,
    type DeliveryParcelExecutionResponse,
    type DeliveryRunDetailsResponse,
    type DeliveryRunResponse,
    type DeliveryRunStatus,
    type DeliveryStopExecutionResponse,
    getDeliveryRunDetails,
    queryDeliveryRuns,
    startDeliveryRun,
    verifyDeliveryCode
} from "@/features/delivery-runs/deliveryRunsApi";
import {type DepotResponse, searchDepots} from "@/features/depots/depotApi";
import {searchVehicles, type VehicleResponse} from "@/features/fleet/fleetApi";
import {
    courierLabel as formatCourierLabel,
    routeLabel,
    runLabel,
    vehicleLabel as formatVehicleLabel
} from "@/features/common/displayFormatters";
import {errorMessage, shortId} from "@/features/common/pageUtils";
import {queryRoutingJobs, type RoutingRouteType} from "@/features/routes/routingApi";
import {type CourierResponse, type PageResponse, searchCouriers} from "@/features/users/userServiceApi";

const PAGE_SIZE = 10;
const runStatusOptions: DeliveryRunStatus[] = ["NOT_STARTED", "IN_PROGRESS", "COMPLETED", "ABORTED"];
const routeTypeOptions: RoutingRouteType[] = ["SENDER_TO_REGIONAL", "REGIONAL_TO_REGIONAL", "REGIONAL_TO_RECEIVER"];
const parcelOutcomeOptions: DeliveryOutcome[] = ["DELIVERED", "FAILED", "REJECTED", "WAITING_PICKUP"];

interface RunFilters {
    courierId: string;
    routeDate: string;
    routeType: "" | RoutingRouteType;
    status: "" | DeliveryRunStatus;
}

interface CompleteParcelDraft {
    outcome: DeliveryOutcome;
    notes: string;
    paymentCollected: string;
}

const initialFilters: RunFilters = {
    courierId: "",
    routeDate: "",
    routeType: "",
    status: ""
};

const initialCompleteDraft: CompleteParcelDraft = {
    outcome: "DELIVERED",
    notes: "",
    paymentCollected: ""
};

export function DeliveryRunsPage() {
    const {user} = useAuth();
    const courierScope = user?.roles.includes("ADMIN") ? "admin" : "dispatcher";

    const [couriers, setCouriers] = useState<CourierResponse[]>([]);
    const [vehicles, setVehicles] = useState<VehicleResponse[]>([]);
    const [depots, setDepots] = useState<DepotResponse[]>([]);
    const [lookupError, setLookupError] = useState<string | null>(null);

    const [runsPage, setRunsPage] = useState<PageResponse<DeliveryRunResponse> | null>(null);
    const [selectedRunId, setSelectedRunId] = useState<string | null>(null);
    const [selectedRunDetails, setSelectedRunDetails] = useState<DeliveryRunDetailsResponse | null>(null);
    const [selectedStopId, setSelectedStopId] = useState<string | null>(null);
    const [selectedParcelId, setSelectedParcelId] = useState<string | null>(null);

    const [filters, setFilters] = useState<RunFilters>(initialFilters);
    const [pageNumber, setPageNumber] = useState(0);
    const [isRunsLoading, setIsRunsLoading] = useState(false);
    const [isDetailsLoading, setIsDetailsLoading] = useState(false);
    const [isRunActionLoading, setIsRunActionLoading] = useState(false);
    const [isStopActionLoading, setIsStopActionLoading] = useState(false);
    const [isParcelActionLoading, setIsParcelActionLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [notice, setNotice] = useState<string | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);
    const [detailsRefreshKey, setDetailsRefreshKey] = useState(0);

    const [verifyAwb, setVerifyAwb] = useState("");
    const [verifyPin, setVerifyPin] = useState("");
    const [completeDraft, setCompleteDraft] = useState<CompleteParcelDraft>(initialCompleteDraft);
    const [runRouteTypeByRunId, setRunRouteTypeByRunId] = useState<Record<string, RoutingRouteType | null>>({});
    const [isRunRouteTypeLookupLoading, setIsRunRouteTypeLookupLoading] = useState(false);

    useEffect(() => {
        const controller = new AbortController();
        setLookupError(null);

        Promise.all([
            searchCouriers({page: 0, size: 500, active: "true"}, courierScope, controller.signal),
            searchVehicles({page: 0, size: 500, sort: "licensePlate,asc"}, controller.signal),
            searchDepots({page: 0, size: 500, sort: "name,asc"}, controller.signal)
        ])
            .then(([courierPage, vehiclePage, depotPage]) => {
                if (controller.signal.aborted) {
                    return;
                }

                setCouriers(courierPage.content);
                setVehicles(vehiclePage.content);
                setDepots(depotPage.content);
            })
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setLookupError(errorMessage(loadError, "Could not load courier, vehicle, or depot options."));
                }
            });

        return () => controller.abort();
    }, [courierScope]);

    useEffect(() => {
        const controller = new AbortController();
        setIsRunsLoading(true);
        setError(null);

        queryDeliveryRuns(
            {
                courierId: filters.courierId.trim() || undefined,
                routeDate: filters.routeDate || undefined,
                status: filters.status || undefined,
                page: pageNumber,
                size: PAGE_SIZE,
                sort: "routeDate,desc"
            },
            controller.signal
        )
            .then((page) => {
                if (!controller.signal.aborted) {
                    setRunsPage(page);
                }
            })
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setRunsPage(null);
                    setError(errorMessage(loadError, "Could not load delivery runs."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsRunsLoading(false);
                }
            });

        return () => controller.abort();
    }, [filters.courierId, filters.routeDate, filters.status, pageNumber, refreshKey]);

    useEffect(() => {
        if (!runsPage) {
            setSelectedRunId(null);
            return;
        }

        if (runsPage.content.some((run) => run.id === selectedRunId)) {
            return;
        }

        setSelectedRunId(runsPage.content[0]?.id ?? null);
    }, [runsPage, selectedRunId]);

    useEffect(() => {
        if (!selectedRunId) {
            setSelectedRunDetails(null);
            setSelectedStopId(null);
            setSelectedParcelId(null);
            return;
        }

        const controller = new AbortController();
        setIsDetailsLoading(true);

        getDeliveryRunDetails(selectedRunId, controller.signal)
            .then((details) => {
                if (!controller.signal.aborted) {
                    setSelectedRunDetails(details);
                }
            })
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setSelectedRunDetails(null);
                    setSelectedStopId(null);
                    setSelectedParcelId(null);
                    setError(errorMessage(loadError, "Could not load delivery run details."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsDetailsLoading(false);
                }
            });

        return () => controller.abort();
    }, [selectedRunId, detailsRefreshKey]);

    useEffect(() => {
        if (!selectedRunDetails) {
            setSelectedStopId(null);
            return;
        }

        if (selectedStopId && selectedRunDetails.stops.some((stop) => stop.id === selectedStopId)) {
            return;
        }

        setSelectedStopId(selectedRunDetails.currentStop?.id ?? selectedRunDetails.stops[0]?.id ?? null);
    }, [selectedRunDetails, selectedStopId]);

    const selectedStop = useMemo(() => {
        if (!selectedRunDetails || !selectedStopId) {
            return null;
        }
        return selectedRunDetails.stops.find((stop) => stop.id === selectedStopId) ?? null;
    }, [selectedRunDetails, selectedStopId]);

    useEffect(() => {
        if (!selectedStop) {
            setSelectedParcelId(null);
            setVerifyAwb("");
            return;
        }

        if (selectedParcelId && selectedStop.parcels.some((parcel) => parcel.id === selectedParcelId)) {
            return;
        }

        const nextParcel =
            selectedStop.parcels.find((parcel) => parcel.completedAt === null) ??
            selectedStop.parcels[0] ??
            null;
        setSelectedParcelId(nextParcel?.id ?? null);
        setVerifyAwb(nextParcel?.awb ?? "");
    }, [selectedParcelId, selectedStop]);

    const selectedRun = useMemo(
        () => runsPage?.content.find((run) => run.id === selectedRunId) ?? null,
        [runsPage, selectedRunId]
    );
    const selectedRunResolvedRouteType = selectedRun ? runRouteTypeByRunId[selectedRun.id] ?? null : null;

    const selectedParcel = useMemo(() => {
        if (!selectedStop || !selectedParcelId) {
            return null;
        }
        return selectedStop.parcels.find((parcel) => parcel.id === selectedParcelId) ?? null;
    }, [selectedParcelId, selectedStop]);

    const courierById = useMemo(
        () =>
            new Map(
                couriers.map((courier) => [
                    courier.courierProfileId,
                    courier
                ])
            ),
        [couriers]
    );

    const vehicleById = useMemo(
        () => new Map(vehicles.map((vehicle) => [vehicle.id, vehicle])),
        [vehicles]
    );

    const depotById = useMemo(
        () => new Map(depots.map((depot) => [depot.id, depot])),
        [depots]
    );

    useEffect(() => {
        setRunRouteTypeByRunId({});

        const runs = runsPage?.content ?? [];
        if (runs.length === 0 || depots.length === 0 || vehicles.length === 0) {
            setIsRunRouteTypeLookupLoading(false);
            return;
        }

        const controller = new AbortController();
        setIsRunRouteTypeLookupLoading(true);

        const routingQueryByKey = new Map<string, { routeDate: string; depotCode: string }>();
        const keyByRunId = new Map<string, string>();

        for (const run of runs) {
            const vehicle = vehicleById.get(run.vehicleId);
            const depotCode = vehicle ? depotById.get(vehicle.depotId)?.depotCode?.trim() ?? "" : "";

            if (!depotCode) {
                continue;
            }

            const key = `${run.routeDate}|${depotCode}`;
            keyByRunId.set(run.id, key);
            if (!routingQueryByKey.has(key)) {
                routingQueryByKey.set(key, {routeDate: run.routeDate, depotCode});
            }
        }

        Promise.all(
            Array.from(routingQueryByKey.entries()).map(([key, query]) =>
                queryRoutingJobs(
                    {
                        routeDate: query.routeDate,
                        depotCode: query.depotCode,
                        status: "SELECTED",
                        page: 0,
                        size: 200,
                        sort: "routeDate,desc"
                    },
                    controller.signal
                )
                    .then((jobsPage) => {
                        const routeTypes = Array.from(new Set((jobsPage.content ?? []).map((job) => job.routeType)));
                        return {key, routeType: routeTypes.length === 1 ? routeTypes[0] : null};
                    })
                    .catch(() => ({key, routeType: null}))
            )
        )
            .then((resolvedRouteTypes) => {
                if (controller.signal.aborted) {
                    return;
                }

                const routeTypeByKey = new Map<string, RoutingRouteType | null>();
                resolvedRouteTypes.forEach((entry) => {
                    routeTypeByKey.set(entry.key, entry.routeType);
                });

                const nextRouteTypeByRunId: Record<string, RoutingRouteType | null> = {};
                runs.forEach((run) => {
                    const key = keyByRunId.get(run.id);
                    nextRouteTypeByRunId[run.id] = key ? routeTypeByKey.get(key) ?? null : null;
                });
                setRunRouteTypeByRunId(nextRouteTypeByRunId);
            })
            .catch(() => {
                if (!controller.signal.aborted) {
                    setRunRouteTypeByRunId({});
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsRunRouteTypeLookupLoading(false);
                }
            });

        return () => controller.abort();
    }, [depotById, depots.length, runsPage, vehicleById, vehicles.length]);

    const filteredRuns = useMemo(() => {
        const runs = runsPage?.content ?? [];
        if (!filters.routeType) {
            return runs;
        }

        return runs.filter((run) => runRouteTypeByRunId[run.id] === filters.routeType);
    }, [filters.routeType, runRouteTypeByRunId, runsPage]);

    useEffect(() => {
        if (!runsPage) {
            return;
        }

        if (filteredRuns.some((run) => run.id === selectedRunId)) {
            return;
        }

        setSelectedRunId(filteredRuns[0]?.id ?? null);
    }, [filteredRuns, runsPage, selectedRunId]);

    const sortedCourierOptions = useMemo(
        () =>
            [...couriers].sort((first, second) =>
                `${first.user.firstName} ${first.user.lastName}`.localeCompare(
                    `${second.user.firstName} ${second.user.lastName}`
                )
            ),
        [couriers]
    );

    const runRows = useMemo(() => {
        const rows = filteredRuns.map((run) => [
            <button
                key={`${run.id}-select`}
                type="button"
                className={run.id === selectedRunId ? "text-link active-row" : "text-link"}
                onClick={() => {
                    setSelectedRunId(run.id);
                    setNotice(null);
                    setError(null);
                }}
            >
                {shortId(run.id)}
            </button>,
            shortId(run.routeId),
            run.routeDate,
            courierLabel(run.courierId, courierById),
            vehicleLabel(run.vehicleId, vehicleById),
            <Tag key={`${run.id}-status`} text={run.status} tone={runStatusTone(run.status)}/>,
            String(run.totalParcelCount)
        ]);

        if (rows.length > 0) {
            return rows;
        }

        return [["No delivery runs found for the selected filters.", "-", "-", "-", "-", "-", "-"]];
    }, [courierById, filteredRuns, selectedRunId, vehicleById]);

    const stopRows = useMemo(() => {
        const rows = (selectedRunDetails?.stops ?? []).map((stop) => [
            <button
                key={`${stop.id}-select`}
                type="button"
                className={stop.id === selectedStopId ? "text-link active-row" : "text-link"}
                onClick={() => {
                    setSelectedStopId(stop.id);
                    setNotice(null);
                    setError(null);
                }}
            >
                {`#${stop.stopOrder}`}
            </button>,
            shortId(stop.routeStopId),
            <Tag key={`${stop.id}-status`} text={stop.status} tone={stopStatusTone(stop.status)}/>,
            String(stop.parcelCount),
            `${formatNumber(stop.totalWeight)} kg / ${formatNumber(stop.totalVolume)} m3`
        ]);

        if (rows.length > 0) {
            return rows;
        }

        return [["No stops available for the selected run.", "-", "-", "-", "-"]];
    }, [selectedRunDetails, selectedStopId]);

    const parcelRows = useMemo(() => {
        const rows = (selectedStop?.parcels ?? []).map((parcel) => [
            <button
                key={`${parcel.id}-select`}
                type="button"
                className={parcel.id === selectedParcelId ? "text-link active-row" : "text-link"}
                onClick={() => {
                    setSelectedParcelId(parcel.id);
                    setVerifyAwb(parcel.awb);
                    setNotice(null);
                    setError(null);
                }}
            >
                {parcel.awb || shortId(parcel.id)}
            </button>,
            parcel.receiverName || "-",
            parcel.receiverPhone || "-",
            parcel.outcome ?
                <Tag key={`${parcel.id}-outcome`} text={parcel.outcome} tone={outcomeTone(parcel.outcome)}/> :
                <Tag key={`${parcel.id}-outcome-empty`} text="PENDING" tone="warning"/>,
            <Tag
                key={`${parcel.id}-verified`}
                text={parcel.deliveryCodeVerified ? "VERIFIED" : "NOT VERIFIED"}
                tone={parcel.deliveryCodeVerified ? "success" : "warning"}
            />,
            parcel.completedAt ? formatTimestamp(parcel.completedAt) : "-"
        ]);

        if (rows.length > 0) {
            return rows;
        }

        return [["No parcels found for the selected stop.", "-", "-", "-", "-", "-"]];
    }, [selectedParcelId, selectedStop]);

    const canArriveAtSelectedStop =
        Boolean(selectedRun) &&
        Boolean(selectedRunDetails) &&
        Boolean(selectedStop) &&
        selectedRun?.status === "IN_PROGRESS" &&
        selectedStop?.status === "PENDING" &&
        selectedRunDetails?.currentStop?.id === selectedStop?.id;

    async function handleRunAction(action: "start" | "complete" | "abort") {
        if (!selectedRun) {
            setError("Select a delivery run first.");
            return;
        }

        setError(null);
        setNotice(null);
        setIsRunActionLoading(true);

        try {
            if (action === "start") {
                await startDeliveryRun(selectedRun.id);
                setNotice(`${runLabel(selectedRun.id)} started.`);
            } else if (action === "complete") {
                await completeDeliveryRun(selectedRun.id);
                setNotice(`${runLabel(selectedRun.id)} completed.`);
            } else {
                await abortDeliveryRun(selectedRun.id);
                setNotice(`${runLabel(selectedRun.id)} aborted.`);
            }

            refreshRuns();
            refreshDetails();
        } catch (runError) {
            setError(errorMessage(runError, "Could not update delivery run status."));
        } finally {
            setIsRunActionLoading(false);
        }
    }

    async function handleArriveAtStop() {
        if (!selectedRun || !selectedStop) {
            setError("Select a run and stop first.");
            return;
        }

        setError(null);
        setNotice(null);
        setIsStopActionLoading(true);

        try {
            const updated = await arriveAtDeliveryStop(selectedRun.id, selectedStop.id);
            setSelectedRunDetails(updated);
            setNotice(`Stop #${selectedStop.stopOrder} marked as arrived.`);
        } catch (arriveError) {
            setError(errorMessage(arriveError, "Could not mark arrival at stop."));
        } finally {
            setIsStopActionLoading(false);
        }
    }

    async function handleVerifyCode(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (!selectedRun || !selectedStop) {
            setError("Select a run and stop first.");
            return;
        }

        const awb = verifyAwb.trim();
        const pin = verifyPin.trim();

        if (!awb || !pin) {
            setError("AWB and PIN are required to verify delivery code.");
            return;
        }

        setError(null);
        setNotice(null);
        setIsParcelActionLoading(true);

        try {
            const verified = await verifyDeliveryCode(selectedRun.id, selectedStop.id, {awb, pin});
            setNotice(`Delivery code verified for ${verified.awb || awb}.`);
            setVerifyPin("");
            refreshDetails();
        } catch (verifyError) {
            setError(errorMessage(verifyError, "Could not verify delivery code."));
        } finally {
            setIsParcelActionLoading(false);
        }
    }

    async function handleCompleteParcel(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (!selectedRun || !selectedParcel) {
            setError("Select a parcel first.");
            return;
        }

        if (selectedParcel.completedAt) {
            setError("Selected parcel is already completed.");
            return;
        }

        let paymentCollected: number | undefined;
        const paymentValue = completeDraft.paymentCollected.trim();

        if (completeDraft.outcome === "DELIVERED" && paymentValue.length > 0) {
            const parsed = Number(paymentValue);
            if (!Number.isFinite(parsed)) {
                setError("Payment collected must be a valid number.");
                return;
            }
            paymentCollected = parsed;
        }

        setError(null);
        setNotice(null);
        setIsParcelActionLoading(true);

        try {
            const notes = completeDraft.notes.trim();
            const payload = {
                outcome: completeDraft.outcome,
                ...(notes ? {notes} : {}),
                ...(paymentCollected !== undefined ? {paymentCollected} : {})
            };

            const completed = await completeDeliveryParcel(selectedRun.id, selectedParcel.id, payload);
            setNotice(`Parcel ${completed.awb || shortId(completed.id)} marked as ${completed.outcome}.`);
            refreshDetails();
        } catch (completeError) {
            setError(errorMessage(completeError, "Could not complete parcel execution."));
        } finally {
            setIsParcelActionLoading(false);
        }
    }

    function refreshRuns() {
        setRefreshKey((current) => current + 1);
    }

    function refreshDetails() {
        setDetailsRefreshKey((current) => current + 1);
    }

    return (
        <div className="page-stack">
            <PageHeader
                title="Delivery Runs"
                subtitle="Live delivery execution monitor wired to delivery-execution run, stop, and parcel endpoints."
                actions={<Tag text={isRunsLoading ? "Loading..." : `${runsPage?.totalElements ?? 0} runs`}
                              tone="neutral"/>}
            />

            <section className="toolbar-row">
                <div className="filter-row">
                    <select
                        className="role-select"
                        value={filters.courierId}
                        onChange={(event) => {
                            setPageNumber(0);
                            setFilters((current) => ({...current, courierId: event.target.value}));
                        }}
                    >
                        <option value="">Any courier</option>
                        {sortedCourierOptions.map((courier) => (
                            <option key={courier.courierProfileId} value={courier.courierProfileId}>
                                {courierLabel(courier.courierProfileId, courierById)}
                            </option>
                        ))}
                    </select>
                    <input
                        type="date"
                        className="text-input"
                        value={filters.routeDate}
                        onChange={(event) => {
                            setPageNumber(0);
                            setFilters((current) => ({...current, routeDate: event.target.value}));
                        }}
                    />
                    <select
                        className="role-select"
                        value={filters.routeType}
                        onChange={(event) => {
                            setPageNumber(0);
                            setFilters((current) => ({
                                ...current,
                                routeType: event.target.value as "" | RoutingRouteType
                            }));
                        }}
                    >
                        <option value="">Any route type</option>
                        {routeTypeOptions.map((routeType) => (
                            <option key={routeType} value={routeType}>
                                {routeType}
                            </option>
                        ))}
                    </select>
                    <select
                        className="role-select"
                        value={filters.status}
                        onChange={(event) => {
                            setPageNumber(0);
                            setFilters((current) => ({
                                ...current,
                                status: event.target.value as "" | DeliveryRunStatus
                            }));
                        }}
                    >
                        <option value="">Any status</option>
                        {runStatusOptions.map((status) => (
                            <option key={status} value={status}>
                                {status}
                            </option>
                        ))}
                    </select>
                </div>
                <button
                    type="button"
                    className="secondary-btn"
                    onClick={() => {
                        setFilters(initialFilters);
                        setPageNumber(0);
                    }}
                >
                    Clear Filters
                </button>
            </section>

            <PageFeedback error={error} notice={notice} errors={lookupError ? [lookupError] : []}/>

            <section className="two-column-grid">
                <TableCard
                    title="Run Monitoring"
                    caption={
                        isRunsLoading
                            ? "Loading delivery runs..."
                            : isRunRouteTypeLookupLoading
                                ? "Resolving route types..."
                                : "Click a run to inspect details"
                    }
                    columns={["Run", "Route", "Date", "Courier", "Vehicle", "Status", "Parcels"]}
                    rows={runRows}
                    loading={isRunsLoading}
                    footer={runsPage ? <PaginationControls page={runsPage} onPageChange={setPageNumber}/> : null}
                />

                <section className="table-card">
                    <div className="table-card-head">
                        <h3>Run Details</h3>
                        <p>Lifecycle controls, operational totals, and current-stop context</p>
                    </div>
                    {!selectedRun ? (
                        <div className="detail-block">
                            <p>Select a delivery run to inspect details.</p>
                        </div>
                    ) : (
                        <div className="detail-block">
                            <p>
                                <strong>Run:</strong> {runLabel(selectedRun.id)}
                            </p>
                            <p>
                                <strong>Route:</strong> {routeLabel(selectedRun.routeId)}
                            </p>
                            <p>
                                <strong>Route date:</strong> {selectedRun.routeDate}
                            </p>
                            <p>
                                <strong>Route
                                    type:</strong> {isRunRouteTypeLookupLoading ? "Loading..." : selectedRunResolvedRouteType ?? "-"}
                            </p>
                            <p>
                                <strong>Courier:</strong> {courierLabel(selectedRun.courierId, courierById)}
                            </p>
                            <p>
                                <strong>Vehicle:</strong> {vehicleLabel(selectedRun.vehicleId, vehicleById)}
                            </p>
                            <p>
                                <strong>Status:</strong> <Tag text={selectedRun.status}
                                                              tone={runStatusTone(selectedRun.status)}/>
                            </p>
                            <p>
                                <strong>Started:</strong> {selectedRun.startedAt ? formatTimestamp(selectedRun.startedAt) : "-"}
                            </p>
                            <p>
                                <strong>Finished:</strong> {selectedRun.finishedAt ? formatTimestamp(selectedRun.finishedAt) : "-"}
                            </p>
                            <p>
                                <strong>Total load:</strong> {selectedRun.totalParcelCount} parcels,
                                {" "}{formatNumber(selectedRun.totalWeight)} kg, {formatNumber(selectedRun.totalVolume)} m3
                            </p>
                            <RunRouteMap googleMapsUrl={selectedRun.googleMapsUrl}/>
                            <p>
                                <strong>Current stop:</strong>{" "}
                                {selectedRunDetails?.currentStop ? `#${selectedRunDetails.currentStop.stopOrder}` : "None"}
                            </p>
                            <div className="action-row">
                                <button
                                    className="secondary-btn"
                                    type="button"
                                    onClick={() => void handleRunAction("start")}
                                    disabled={isRunActionLoading || selectedRun.status !== "NOT_STARTED"}
                                >
                                    {isRunActionLoading ? "Saving..." : "Start Run"}
                                </button>
                                <button
                                    className="secondary-btn"
                                    type="button"
                                    onClick={() => void handleRunAction("complete")}
                                    disabled={isRunActionLoading || selectedRun.status !== "IN_PROGRESS"}
                                >
                                    {isRunActionLoading ? "Saving..." : "Complete Run"}
                                </button>
                                <button
                                    className="secondary-btn"
                                    type="button"
                                    onClick={() => void handleRunAction("abort")}
                                    disabled={isRunActionLoading || (selectedRun.status !== "NOT_STARTED" && selectedRun.status !== "IN_PROGRESS")}
                                >
                                    {isRunActionLoading ? "Saving..." : "Abort Run"}
                                </button>
                                <button
                                    className="secondary-btn"
                                    type="button"
                                    onClick={refreshDetails}
                                    disabled={isDetailsLoading}
                                >
                                    {isDetailsLoading ? "Refreshing..." : "Refresh Details"}
                                </button>
                            </div>
                        </div>
                    )}
                </section>
            </section>

            <section className="two-column-grid">
                <TableCard
                    title="Stop Execution"
                    caption={isDetailsLoading ? "Loading stop details..." : "Select a stop to manage parcel execution"}
                    columns={["Stop", "Route Stop", "Status", "Parcels", "Load"]}
                    rows={stopRows}
                    loading={isDetailsLoading}
                    headerAction={
                        <button
                            type="button"
                            className="secondary-btn"
                            onClick={() => void handleArriveAtStop()}
                            disabled={!canArriveAtSelectedStop || isStopActionLoading}
                        >
                            {isStopActionLoading ? "Arriving..." : "Arrive at selected stop"}
                        </button>
                    }
                />

                <section className="table-card">
                    <div className="table-card-head">
                        <h3>Parcel Execution</h3>
                        <p>Verify delivery code and complete parcel outcomes</p>
                    </div>
                    {!selectedStop ? (
                        <div className="detail-block">
                            <p>Select a stop to inspect parcels.</p>
                        </div>
                    ) : (
                        <>
                            <div className="detail-block">
                                <p>
                                    <strong>Selected stop:</strong> #{selectedStop.stopOrder}
                                </p>
                                <p>
                                    <strong>Coordinates:</strong> {selectedStop.latitude}, {selectedStop.longitude}
                                </p>
                                <p>
                                    <strong>Status:</strong>{" "}
                                    <Tag text={selectedStop.status} tone={stopStatusTone(selectedStop.status)}/>
                                </p>
                            </div>

                            <div className="table-wrap">
                                <table>
                                    <thead>
                                    <tr>
                                        <th>Parcel</th>
                                        <th>Receiver</th>
                                        <th>Phone</th>
                                        <th>Outcome</th>
                                        <th>Code</th>
                                        <th>Completed</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {parcelRows.map((row, rowIndex) => (
                                        <tr key={`parcel-row-${rowIndex}`}>
                                            {row.map((cell, cellIndex) => (
                                                <td key={`parcel-cell-${rowIndex}-${cellIndex}`}>{cell}</td>
                                            ))}
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>

                            <form className="detail-form" onSubmit={handleVerifyCode} noValidate>
                                <div className="filter-grid">
                                    <label>
                                        AWB
                                        <input
                                            className="text-input"
                                            value={verifyAwb}
                                            onChange={(event) => setVerifyAwb(event.target.value)}
                                            placeholder="AWB code"
                                            required
                                        />
                                    </label>
                                    <label>
                                        PIN
                                        <input
                                            className="text-input"
                                            value={verifyPin}
                                            onChange={(event) => setVerifyPin(event.target.value)}
                                            placeholder="Delivery PIN"
                                            required
                                        />
                                    </label>
                                </div>
                                <div className="action-row">
                                    <button
                                        type="submit"
                                        className="secondary-btn"
                                        disabled={isParcelActionLoading || !selectedStop}
                                    >
                                        {isParcelActionLoading ? "Verifying..." : "Verify Delivery Code"}
                                    </button>
                                </div>
                            </form>

                            <form className="detail-form" onSubmit={handleCompleteParcel} noValidate>
                                <div className="filter-grid">
                                    <label>
                                        Outcome
                                        <select
                                            className="role-select"
                                            value={completeDraft.outcome}
                                            onChange={(event) =>
                                                setCompleteDraft((current) => ({
                                                    ...current,
                                                    outcome: event.target.value as DeliveryOutcome,
                                                    paymentCollected:
                                                        event.target.value === "DELIVERED" ? current.paymentCollected : ""
                                                }))
                                            }
                                        >
                                            {parcelOutcomeOptions.map((outcome) => (
                                                <option key={outcome} value={outcome}>
                                                    {outcome}
                                                </option>
                                            ))}
                                        </select>
                                    </label>
                                    <label>
                                        Notes
                                        <input
                                            className="text-input"
                                            value={completeDraft.notes}
                                            onChange={(event) =>
                                                setCompleteDraft((current) => ({...current, notes: event.target.value}))
                                            }
                                            placeholder="Optional completion notes"
                                        />
                                    </label>
                                    <label>
                                        Payment Collected
                                        <input
                                            type="number"
                                            className="text-input"
                                            step="0.01"
                                            value={completeDraft.paymentCollected}
                                            disabled={completeDraft.outcome !== "DELIVERED"}
                                            onChange={(event) =>
                                                setCompleteDraft((current) => ({
                                                    ...current,
                                                    paymentCollected: event.target.value
                                                }))
                                            }
                                            placeholder="Optional"
                                        />
                                    </label>
                                </div>
                                <div className="action-row">
                                    <button
                                        type="submit"
                                        className="primary-btn"
                                        disabled={isParcelActionLoading || !selectedParcel || selectedParcel.completedAt !== null}
                                    >
                                        {isParcelActionLoading ? "Saving..." : "Complete Selected Parcel"}
                                    </button>
                                </div>
                            </form>
                        </>
                    )}
                </section>
            </section>
        </div>
    );
}

function courierLabel(courierId: string, courierById: Map<string, CourierResponse>) {
    return formatCourierLabel(courierById.get(courierId), courierId);
}

function vehicleLabel(vehicleId: string, vehicleById: Map<string, VehicleResponse>) {
    return formatVehicleLabel(vehicleById.get(vehicleId), vehicleId);
}

function formatNumber(value: number) {
    return Number.isFinite(value) ? value.toFixed(2).replace(/\.00$/, "") : "0";
}

function formatTimestamp(value: string) {
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
        return value;
    }
    return parsed.toLocaleString();
}

function runStatusTone(status: DeliveryRunStatus): "success" | "warning" | "danger" | "neutral" {
    if (status === "IN_PROGRESS") {
        return "success";
    }
    if (status === "ABORTED") {
        return "danger";
    }
    if (status === "NOT_STARTED") {
        return "warning";
    }
    return "neutral";
}

function stopStatusTone(status: DeliveryStopExecutionResponse["status"]): "success" | "warning" | "neutral" {
    if (status === "IN_PROGRESS") {
        return "success";
    }
    if (status === "PENDING") {
        return "warning";
    }
    return "neutral";
}

function outcomeTone(outcome: DeliveryParcelExecutionResponse["outcome"]): "success" | "warning" | "danger" {
    if (outcome === "DELIVERED") {
        return "success";
    }
    if (outcome === "WAITING_PICKUP") {
        return "warning";
    }
    return "danger";
}

function RunRouteMap({
                         googleMapsUrl
                     }: {
    googleMapsUrl: string;
}) {
    const googleUrl = (googleMapsUrl ?? "").trim();
    if (!googleUrl) {
        return (
            <div className="route-map-block">
                <p>
                    <strong>Map:</strong> route map is not available yet.
                </p>
            </div>
        );
    }

    return (
        <div className="route-map-block">
            <p>
                <strong>Map:</strong>{" "}
                <a className="route-map-link" href={googleUrl} target="_blank" rel="noreferrer">
                    Open in Google Maps
                </a>
            </p>
        </div>
    );
}

