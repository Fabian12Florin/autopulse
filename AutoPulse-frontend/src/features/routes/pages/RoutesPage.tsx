import {type FormEvent, useEffect, useMemo, useState} from "react";
import {PageHeader} from "@/components/PageHeader";
import {PageFeedback} from "@/components/PageFeedback";
import {TableCard} from "@/components/TableCard";
import {Tag} from "@/components/Tag";
import {PaginationControls} from "@/components/PaginationControls";
import {type DepotResponse, searchDepots} from "@/features/depots/depotApi";
import {
    createRoutingJob,
    queryRoutingJobs,
    type RoutingJobResponse,
    type RoutingJobStatus,
    type RoutingRouteType,
    selectRoutingJob
} from "@/features/routes/routingApi";
import {depotLabelFromMap} from "@/features/common/displayFormatters";
import {errorMessage, shortId} from "@/features/common/pageUtils";
import {type PageResponse, searchUsers, type UserResponse} from "@/features/users/userServiceApi";

const PAGE_SIZE = 10;
const routeTypeOptions: RoutingRouteType[] = ["SENDER_TO_REGIONAL", "REGIONAL_TO_REGIONAL", "REGIONAL_TO_RECEIVER"];
const routingStatusOptions: RoutingJobStatus[] = ["PENDING", "SELECTED"];

interface CreateRouteForm {
    depotCode: string;
    routeDate: string;
    routeType: RoutingRouteType;
}

const initialCreateRouteForm: CreateRouteForm = {
    depotCode: "",
    routeDate: formatDateForInput(new Date()),
    routeType: "REGIONAL_TO_RECEIVER"
};

export function RoutesPage() {
    const [depotOptions, setDepotOptions] = useState<DepotResponse[]>([]);
    const [userOptions, setUserOptions] = useState<UserResponse[]>([]);
    const [routingJobsPage, setRoutingJobsPage] = useState<PageResponse<RoutingJobResponse> | null>(null);
    const [selectedRoutingJobId, setSelectedRoutingJobId] = useState<string | null>(null);
    const [pageNumber, setPageNumber] = useState(0);
    const [filters, setFilters] = useState({
        depotCode: "",
        routeDate: "",
        routeType: "" as "" | RoutingRouteType,
        status: "" as "" | RoutingJobStatus
    });
    const [createForm, setCreateForm] = useState<CreateRouteForm>(initialCreateRouteForm);
    const [isLoading, setIsLoading] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [notice, setNotice] = useState<string | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);

    useEffect(() => {
        const controller = new AbortController();

        void Promise.allSettled([
            searchDepots({page: 0, size: 500, sort: "name,asc"}, controller.signal),
            searchUsers({page: 0, size: 500}, controller.signal)
        ]).then(([depotsResult, usersResult]) => {
            if (controller.signal.aborted) {
                return;
            }

            if (depotsResult.status === "fulfilled") {
                setDepotOptions(depotsResult.value.content);
                setCreateForm((current) => {
                    if (current.depotCode || depotsResult.value.content.length === 0) {
                        return current;
                    }
                    return {...current, depotCode: depotsResult.value.content[0].depotCode ?? ""};
                });
            } else {
                setError(errorMessage(depotsResult.reason, "Could not load depot options."));
            }

            if (usersResult.status === "fulfilled") {
                setUserOptions(usersResult.value.content);
            }
        });

        return () => controller.abort();
    }, []);

    useEffect(() => {
        const controller = new AbortController();
        setIsLoading(true);
        setError(null);

        queryRoutingJobs(
            {
                depotCode: filters.depotCode.trim() || undefined,
                routeDate: filters.routeDate || undefined,
                routeType: filters.routeType || undefined,
                status: filters.status || undefined,
                page: pageNumber,
                size: PAGE_SIZE,
                sort: "routeDate,desc"
            },
            controller.signal
        )
            .then((page) => {
                if (!controller.signal.aborted) {
                    setRoutingJobsPage(page);
                }
            })
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setRoutingJobsPage(null);
                    setError(errorMessage(loadError, "Could not load routing jobs."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsLoading(false);
                }
            });

        return () => controller.abort();
    }, [filters.depotCode, filters.routeDate, filters.routeType, filters.status, pageNumber, refreshKey]);

    useEffect(() => {
        if (!routingJobsPage) {
            return;
        }

        if (routingJobsPage.content.some((job) => job.id === selectedRoutingJobId)) {
            return;
        }

        setSelectedRoutingJobId(routingJobsPage.content[0]?.id ?? null);
    }, [routingJobsPage, selectedRoutingJobId]);

    const selectedRoutingJob = useMemo(
        () => routingJobsPage?.content.find((job) => job.id === selectedRoutingJobId) ?? null,
        [routingJobsPage, selectedRoutingJobId]
    );

    const depotById = useMemo(
        () => new Map(depotOptions.map((depot) => [depot.id, depot])),
        [depotOptions]
    );

    const userById = useMemo(
        () => new Map(userOptions.map((entry) => [entry.id, entry])),
        [userOptions]
    );

    const routingJobRows = useMemo(() => {
        const rows = (routingJobsPage?.content ?? []).map((job) => [
            <button
                key={job.id}
                type="button"
                className={job.id === selectedRoutingJobId ? "text-link active-row" : "text-link"}
                onClick={() => setSelectedRoutingJobId(job.id)}
            >
                {shortId(job.id)}
            </button>,
            depotLabelFromMap(job.depotId, depotById),
            job.routeDate,
            job.routeType,
            <Tag key={`${job.id}-status`} text={job.routingJobStatus} tone={statusTone(job.routingJobStatus)}/>,
            `${job.assignedParcelCount}/${job.inputParcelCount}`,
            String(job.numberOfRoutes)
        ]);

        if (rows.length > 0) {
            return rows;
        }

        return [["No routing jobs found for the selected filters.", "-", "-", "-", "-", "-", "-"]];
    }, [depotById, routingJobsPage, selectedRoutingJobId]);

    async function handleCreateRoutingJob(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setNotice(null);

        if (!createForm.depotCode.trim()) {
            setError("Depot is required.");
            return;
        }

        if (!createForm.routeDate) {
            setError("Route date is required.");
            return;
        }

        setIsSaving(true);

        try {
            const created = await createRoutingJob(createForm);
            setSelectedRoutingJobId(created.id);
            setPageNumber(0);
            setNotice(`Routing job ${shortId(created.id)} created.`);
            refresh();
        } catch (createError) {
            setError(errorMessage(createError, "Could not create routing job."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleSelectRoutingJob() {
        if (!selectedRoutingJob) {
            setError("Select a routing job first.");
            return;
        }

        setError(null);
        setNotice(null);
        setIsSaving(true);

        try {
            const updated = await selectRoutingJob(selectedRoutingJob.id);
            setRoutingJobsPage((current) => {
                if (!current) {
                    return current;
                }

                return {
                    ...current,
                    content: current.content.map((job) => (job.id === updated.id ? updated : job))
                };
            });
            setNotice(`Routing job ${shortId(updated.id)} selected and published.`);
        } catch (selectError) {
            setError(errorMessage(selectError, "Could not select routing job."));
        } finally {
            setIsSaving(false);
        }
    }

    function refresh() {
        setRefreshKey((current) => current + 1);
    }

    return (
        <div className="page-stack">
            <PageHeader
                title="Routes"
                subtitle="Simple routing-job control mapped directly to routing-service query, create, and select endpoints."
                actions={<Tag text={isLoading ? "Loading..." : `${routingJobsPage?.totalElements ?? 0} jobs`}
                              tone="neutral"/>}
            />

            <section className="toolbar-row">
                <div className="filter-row">
                    <select
                        className="role-select"
                        value={filters.depotCode}
                        onChange={(event) => {
                            setPageNumber(0);
                            setFilters((current) => ({...current, depotCode: event.target.value}));
                        }}
                    >
                        <option value="">Any depot</option>
                        {depotOptions
                            .filter((depot) => Boolean(depot.depotCode?.trim()))
                            .map((depot) => (
                                <option key={depot.id} value={depot.depotCode}>
                                    {depot.depotCode} ({depot.name})
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
                        <option value="">Any type</option>
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
                                status: event.target.value as "" | RoutingJobStatus
                            }));
                        }}
                    >
                        <option value="">Any status</option>
                        {routingStatusOptions.map((status) => (
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
                        setPageNumber(0);
                        setFilters({depotCode: "", routeDate: "", routeType: "", status: ""});
                    }}
                >
                    Clear Filters
                </button>
            </section>

            <PageFeedback error={error} notice={notice}/>

            <section className="two-column-grid">
                <TableCard
                    title="Routing Jobs"
                    caption={isLoading ? "Loading routing jobs..." : "Click a job for details"}
                    columns={["Job", "Depot", "Date", "Type", "Status", "Assigned", "Routes"]}
                    rows={routingJobRows}
                    loading={isLoading}
                    footer={
                        routingJobsPage ? (
                            <PaginationControls page={routingJobsPage} onPageChange={setPageNumber}/>
                        ) : null
                    }
                />

                <section className="table-card">
                    <div className="table-card-head">
                        <h3>Selected Job</h3>
                        <p>Backend fields returned by routing-service</p>
                    </div>
                    {selectedRoutingJob ? (
                        <div className="detail-block">
                            <p>
                                <strong>Job:</strong> {selectedRoutingJob.id}
                            </p>
                            <p>
                                <strong>Generated by user:</strong>{" "}
                                {generatedByLabel(selectedRoutingJob.generatedByUserId, userById)}
                            </p>
                            <p>
                                <strong>Depot:</strong> {depotLabelFromMap(selectedRoutingJob.depotId, depotById)}
                            </p>
                            <p>
                                <strong>Route date:</strong> {selectedRoutingJob.routeDate}
                            </p>
                            <p>
                                <strong>Route type:</strong> {selectedRoutingJob.routeType}
                            </p>
                            <p>
                                <strong>Status:</strong>{" "}
                                <Tag text={selectedRoutingJob.routingJobStatus}
                                     tone={statusTone(selectedRoutingJob.routingJobStatus)}/>
                            </p>
                            <p>
                                <strong>Input
                                    capacity:</strong> {selectedRoutingJob.inputParcelCount} parcels, {selectedRoutingJob.inputCourierCount} couriers, {selectedRoutingJob.inputVehicleCount} vehicles
                            </p>
                            <p>
                                <strong>Assignment result:</strong> {selectedRoutingJob.assignedParcelCount} assigned
                                / {selectedRoutingJob.unassignedParcelCount} unassigned
                            </p>
                            <p>
                                <strong>Planned routes:</strong> {selectedRoutingJob.numberOfRoutes}
                            </p>
                            <div className="action-row">
                                <button
                                    type="button"
                                    className="secondary-btn"
                                    onClick={() => void handleSelectRoutingJob()}
                                    disabled={isSaving || selectedRoutingJob.routingJobStatus === "SELECTED"}
                                >
                                    {isSaving ? "Saving..." : "Select & Publish"}
                                </button>
                            </div>
                        </div>
                    ) : (
                        <div className="detail-block">
                            <p>Select a routing job to inspect details.</p>
                        </div>
                    )}
                </section>
            </section>

            <section className="table-card">
                <div className="table-card-head">
                    <h3>Generate Routing Job</h3>
                    <p>Create a routing job using routing-service POST /routing/routing-jobs/create</p>
                </div>
                <form className="detail-form" onSubmit={handleCreateRoutingJob} noValidate>
                    <div className="filter-grid">
                        <label>
                            Depot
                            <select
                                className="role-select"
                                value={createForm.depotCode}
                                onChange={(event) => setCreateForm((current) => ({
                                    ...current,
                                    depotCode: event.target.value
                                }))}
                                required
                            >
                                <option value="">Select depot</option>
                                {depotOptions.filter((depot) => Boolean(depot.depotCode?.trim())).map((depot) => (
                                    <option key={depot.id} value={depot.depotCode}>
                                        {depot.depotCode} ({depot.name})
                                    </option>
                                ))}
                            </select>
                        </label>
                        <label>
                            Route date
                            <input
                                type="date"
                                className="text-input"
                                value={createForm.routeDate}
                                onChange={(event) => setCreateForm((current) => ({
                                    ...current,
                                    routeDate: event.target.value
                                }))}
                                required
                            />
                        </label>
                        <label>
                            Route type
                            <select
                                className="role-select"
                                value={createForm.routeType}
                                onChange={(event) => setCreateForm((current) => ({
                                    ...current,
                                    routeType: event.target.value as RoutingRouteType
                                }))}
                                required
                            >
                                {routeTypeOptions.map((routeType) => (
                                    <option key={routeType} value={routeType}>
                                        {routeType}
                                    </option>
                                ))}
                            </select>
                        </label>
                    </div>
                    <div className="action-row">
                        <button type="submit" className="primary-btn" disabled={isSaving}>
                            {isSaving ? "Creating..." : "Generate Routes"}
                        </button>
                    </div>
                </form>
            </section>
        </div>
    );
}

function statusTone(status: RoutingJobStatus): "success" | "warning" {
    return status === "SELECTED" ? "success" : "warning";
}

function formatDateForInput(date: Date) {
    return date.toISOString().slice(0, 10);
}

function generatedByLabel(userId: string, userById: Map<string, UserResponse>) {
    const user = userById.get(userId);
    if (!user) {
        return `Unknown user (${userId})`;
    }

    const fullName = `${user.firstName} ${user.lastName}`.trim();
    if (!fullName) {
        return `Unknown user (${user.id})`;
    }

    return `${fullName} (${user.id})`;
}


