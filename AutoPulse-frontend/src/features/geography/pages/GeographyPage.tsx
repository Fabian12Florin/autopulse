import {type FormEvent, type ReactNode, useEffect, useState} from "react";
import {PageHeader} from "@/components/PageHeader";
import {PaginationControls} from "@/components/PaginationControls";
import {PageFeedback} from "@/components/PageFeedback";
import {StatCard} from "@/components/StatCard";
import {Tag} from "@/components/Tag";
import {useAuth} from "@/features/auth/AuthContext";
import {errorMessage} from "@/features/common/pageUtils";
import {
    type CoordinatesResponse,
    createLocality,
    createRegion,
    type LocalityPayload,
    type LocalityResponse,
    type RegionPayload,
    type RegionResponse,
    resolveCoordinates,
    searchLocalities,
    searchRegions
} from "@/features/geography/geographyApi";
import type {PageResponse} from "@/features/users/userServiceApi";

type GeographyMode = "regions" | "counties";

const PAGE_SIZE = 10;
const emptyRegionForm: RegionPayload = {name: "", code: ""};
const emptyLocalityForm: LocalityPayload = {name: "", regionCode: ""};
const emptyAddressForm = {street: "", city: "", county: ""};

export function GeographyPage() {
    const {user} = useAuth();
    const isAdmin = Boolean(user?.roles.includes("ADMIN"));
    const [mode, setMode] = useState<GeographyMode>("regions");
    const [regionPage, setRegionPage] = useState<PageResponse<RegionResponse> | null>(null);
    const [localityPage, setLocalityPage] = useState<PageResponse<LocalityResponse> | null>(null);
    const [regionOptions, setRegionOptions] = useState<RegionResponse[]>([]);
    const [regionFilters, setRegionFilters] = useState({name: "", code: ""});
    const [localityFilters, setLocalityFilters] = useState({name: "", regionCode: ""});
    const [regionPageNumber, setRegionPageNumber] = useState(0);
    const [localityPageNumber, setLocalityPageNumber] = useState(0);
    const [regionForm, setRegionForm] = useState<RegionPayload>(emptyRegionForm);
    const [localityForm, setLocalityForm] = useState<LocalityPayload>(emptyLocalityForm);
    const [addressForm, setAddressForm] = useState(emptyAddressForm);
    const [coordinates, setCoordinates] = useState<CoordinatesResponse | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [isResolving, setIsResolving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [notice, setNotice] = useState<string | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        setIsLoading(true);
        setError(null);

        searchRegions(
            {
                page: regionPageNumber,
                size: PAGE_SIZE,
                name: regionFilters.name.trim(),
                code: regionFilters.code.trim()
            },
            controller.signal
        )
            .then(setRegionPage)
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setError(errorMessage(loadError, "Could not load regions."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsLoading(false);
                }
            });

        return () => controller.abort();
    }, [regionFilters.code, regionFilters.name, regionPageNumber, refreshKey]);

    useEffect(() => {
        const controller = new AbortController();
        setError(null);

        searchRegions({page: 0, size: 500}, controller.signal)
            .then((page) => setRegionOptions(page.content))
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setError(errorMessage(loadError, "Could not load region options."));
                }
            });

        return () => controller.abort();
    }, [refreshKey]);

    useEffect(() => {
        const controller = new AbortController();
        setIsLoading(true);
        setError(null);

        searchLocalities(
            {
                page: localityPageNumber,
                size: PAGE_SIZE,
                name: localityFilters.name.trim(),
                regionCode: localityFilters.regionCode.trim()
            },
            controller.signal
        )
            .then(setLocalityPage)
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setError(errorMessage(loadError, "Could not load counties."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsLoading(false);
                }
            });

        return () => controller.abort();
    }, [localityFilters.name, localityFilters.regionCode, localityPageNumber, refreshKey]);

    async function handleRegionSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setNotice(null);

        if (!isAdmin) {
            setError("Only administrators can change geography master data.");
            return;
        }

        const validationError = validateRegionPayload(regionForm);
        if (validationError) {
            setError(validationError);
            return;
        }

        try {
            setIsSaving(true);
            await createRegion(cleanRegionPayload(regionForm));
            setRegionForm(emptyRegionForm);
            setNotice("Region created.");
            refresh();
        } catch (saveError) {
            setError(errorMessage(saveError, "Could not create region."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleLocalitySubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setNotice(null);

        if (!isAdmin) {
            setError("Only administrators can change geography master data.");
            return;
        }

        const validationError = validateLocalityPayload(localityForm);
        if (validationError) {
            setError(validationError);
            return;
        }

        try {
            setIsSaving(true);
            await createLocality(cleanLocalityPayload(localityForm));
            setLocalityForm(emptyLocalityForm);
            setNotice("County created.");
            refresh();
        } catch (saveError) {
            setError(errorMessage(saveError, "Could not create county."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleResolveCoordinates(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setNotice(null);
        setCoordinates(null);

        if (!addressForm.street.trim() || !addressForm.city.trim()) {
            setError("Street and city are required for coordinate lookup.");
            return;
        }

        try {
            setIsResolving(true);
            const result = await resolveCoordinates({
                street: addressForm.street.trim(),
                city: addressForm.city.trim(),
                county: addressForm.county.trim() || undefined
            });
            setCoordinates(result);
            setNotice("Coordinates resolved.");
        } catch (resolveError) {
            setError(errorMessage(resolveError, "Could not resolve coordinates."));
        } finally {
            setIsResolving(false);
        }
    }

    function refresh() {
        setRefreshKey((current) => current + 1);
    }

    return (
        <div className="page-stack">
            <PageHeader
                title="Geography"
                subtitle="Region and county master data, with coordinate lookup for operational addresses."
                actions={<Tag text={isAdmin ? "Admin management" : "Dispatcher read-only"}
                              tone={isAdmin ? "success" : "neutral"}/>}
            />

            <section className="metrics-grid">
                <StatCard label="Regions" value={String(regionPage?.totalElements ?? 0)}
                          trend="Backend /geography/query/regions"/>
                <StatCard label="Counties" value={String(localityPage?.totalElements ?? 0)}
                          trend="Backend /geography/query/counties"/>
                <StatCard label="Mode" value={isAdmin ? "Create only" : "Read-only"}
                          tone={isAdmin ? "positive" : "neutral"}/>
            </section>

            <section className="toolbar-row">
                <div className="segmented-control" aria-label="Geography section">
                    <button type="button" className={mode === "regions" ? "active" : ""}
                            onClick={() => setMode("regions")}>
                        Regions
                    </button>
                    <button type="button" className={mode === "counties" ? "active" : ""}
                            onClick={() => setMode("counties")}>
                        Counties
                    </button>
                </div>

            </section>

            <PageFeedback error={error} notice={notice}/>

            {mode === "regions" ? (
                <section className={isAdmin ? "two-column-grid directory-grid" : "page-stack"}>
                    <DirectoryList
                        title="Regions"
                        total={regionPage?.totalElements ?? 0}
                        loading={isLoading}
                        filters={
                            <div className="filter-grid">
                                <label>
                                    Code
                                    <input
                                        value={regionFilters.code}
                                        onChange={(event) => {
                                            setRegionPageNumber(0);
                                            setRegionFilters((current) => ({...current, code: event.target.value}));
                                        }}
                                        placeholder="RO-B"
                                    />
                                </label>
                                <label>
                                    Name
                                    <input
                                        value={regionFilters.name}
                                        onChange={(event) => {
                                            setRegionPageNumber(0);
                                            setRegionFilters((current) => ({...current, name: event.target.value}));
                                        }}
                                        placeholder="Bucuresti"
                                    />
                                </label>
                            </div>
                        }
                        columns={["Code", "Name", "Created", "Updated"]}
                        rows={(regionPage?.content ?? []).map((region) => [
                            region.code,
                            region.name,
                            formatDate(region.createdAt),
                            formatDate(region.updatedAt)
                        ])}
                        emptyMessage="No regions match the current filters."
                        pagination={regionPage ?
                            <PaginationControls page={regionPage} onPageChange={setRegionPageNumber}/> : null}
                    />

                    {isAdmin ? (
                        <section className="table-card">
                            <div className="table-card-head">
                                <h3>Create Region</h3>
                                <p>Admin-only creation backed by /geography/admin/regions</p>
                            </div>
                            <form className="detail-form" onSubmit={handleRegionSubmit} noValidate>
                                <RegionForm form={regionForm} onChange={setRegionForm}/>
                                <DetailActions isSaving={isSaving}/>
                            </form>
                        </section>
                    ) : null}
                </section>
            ) : (
                <section className={isAdmin ? "two-column-grid directory-grid" : "page-stack"}>
                    <DirectoryList
                        title="Counties"
                        total={localityPage?.totalElements ?? 0}
                        loading={isLoading}
                        filters={
                            <div className="filter-grid">
                                <label>
                                    Name
                                    <input
                                        value={localityFilters.name}
                                        onChange={(event) => {
                                            setLocalityPageNumber(0);
                                            setLocalityFilters((current) => ({...current, name: event.target.value}));
                                        }}
                                        placeholder="Bucuresti"
                                    />
                                </label>
                                <label>
                                    Region
                                    <input
                                        value={localityFilters.regionCode}
                                        onChange={(event) => {
                                            setLocalityPageNumber(0);
                                            setLocalityFilters((current) => ({
                                                ...current,
                                                regionCode: event.target.value
                                            }));
                                        }}
                                        placeholder="RO-B"
                                    />
                                </label>
                            </div>
                        }
                        columns={["Name", "Region", "Created", "Updated"]}
                        rows={(localityPage?.content ?? []).map((locality) => [
                            locality.name,
                            `${locality.region.code} - ${locality.region.name}`,
                            formatDate(locality.createdAt),
                            formatDate(locality.updatedAt)
                        ])}
                        emptyMessage="No counties match the current filters."
                        pagination={localityPage ?
                            <PaginationControls page={localityPage} onPageChange={setLocalityPageNumber}/> : null}
                    />

                    {isAdmin ? (
                        <section className="table-card">
                            <div className="table-card-head">
                                <h3>Create County</h3>
                                <p>Admin-only creation backed by /geography/admin/counties</p>
                            </div>
                            <form className="detail-form" onSubmit={handleLocalitySubmit} noValidate>
                                <LocalityForm form={localityForm} regions={regionOptions} onChange={setLocalityForm}/>
                                <DetailActions isSaving={isSaving}/>
                            </form>
                        </section>
                    ) : null}
                </section>
            )}

            <section className="table-card">
                <div className="table-card-head">
                    <h3>Coordinate Lookup</h3>
                    <p>Read-only geocoding endpoint available to Admin and Dispatcher</p>
                </div>
                <form className="detail-form" onSubmit={handleResolveCoordinates} noValidate>
                    <div className="filter-grid">
                        <label>
                            Street
                            <input
                                value={addressForm.street}
                                onChange={(event) => setAddressForm((current) => ({
                                    ...current,
                                    street: event.target.value
                                }))}
                                placeholder="Leandrului 1"
                                maxLength={200}
                                required
                            />
                        </label>
                        <label>
                            City
                            <input
                                value={addressForm.city}
                                onChange={(event) => setAddressForm((current) => ({
                                    ...current,
                                    city: event.target.value
                                }))}
                                placeholder="Timisoara"
                                maxLength={120}
                                required
                            />
                        </label>
                        <label>
                            County
                            <input
                                value={addressForm.county}
                                onChange={(event) => setAddressForm((current) => ({
                                    ...current,
                                    county: event.target.value
                                }))}
                                placeholder="Timis"
                                maxLength={120}
                            />
                        </label>
                    </div>
                    <div className="action-row">
                        <button type="submit" className="primary-btn" disabled={isResolving}>
                            {isResolving ? "Resolving..." : "Resolve coordinates"}
                        </button>
                    </div>
                    {coordinates ? (
                        <div className="detail-block">
                            <p>
                                <strong>Longitude:</strong> {coordinates.x}
                            </p>
                            <p>
                                <strong>Latitude:</strong> {coordinates.y}
                            </p>
                        </div>
                    ) : null}
                </form>
            </section>
        </div>
    );
}

function DirectoryList({
                           title,
                           total,
                           loading,
                           filters,
                           columns,
                           rows,
                           emptyMessage,
                           pagination
                       }: {
    title: string;
    total: number;
    loading: boolean;
    filters: ReactNode;
    columns: string[];
    rows: ReactNode[][];
    emptyMessage: string;
    pagination: ReactNode;
}) {
    return (
        <section className="table-card">
            <div className="table-card-head">
                <h3>{title}</h3>
                <p>{loading ? "Loading..." : `${total} records found`}</p>
            </div>
            <div className="detail-block">{filters}</div>
            <div className="table-wrap">
                <table>
                    <thead>
                    <tr>
                        {columns.map((column) => (
                            <th key={column}>{column}</th>
                        ))}
                    </tr>
                    </thead>
                    <tbody>
                    {rows.length ? (
                        rows.map((row, rowIndex) => (
                            <tr key={`geography-row-${rowIndex}`}>
                                {row.map((cell, cellIndex) => (
                                    <td key={`geography-cell-${rowIndex}-${cellIndex}`}>{cell}</td>
                                ))}
                            </tr>
                        ))
                    ) : (
                        <tr>
                            <td colSpan={columns.length}>{emptyMessage}</td>
                        </tr>
                    )}
                    </tbody>
                </table>
            </div>
            {pagination}
        </section>
    );
}

function RegionForm({
                        form,
                        onChange
                    }: {
    form: RegionPayload;
    onChange: (nextForm: RegionPayload | ((current: RegionPayload) => RegionPayload)) => void;
}) {
    return (
        <>
            <label>
                Name
                <input
                    value={form.name}
                    onChange={(event) => onChange((current) => ({...current, name: event.target.value}))}
                    minLength={2}
                    maxLength={100}
                    required
                />
            </label>
            <label>
                Code
                <input
                    value={form.code}
                    onChange={(event) => onChange((current) => ({...current, code: event.target.value}))}
                    minLength={2}
                    maxLength={20}
                    required
                />
            </label>
        </>
    );
}

function LocalityForm({
                          form,
                          regions,
                          onChange
                      }: {
    form: LocalityPayload;
    regions: RegionResponse[];
    onChange: (nextForm: LocalityPayload | ((current: LocalityPayload) => LocalityPayload)) => void;
}) {
    return (
        <>
            <label>
                County name
                <input
                    value={form.name}
                    onChange={(event) => onChange((current) => ({...current, name: event.target.value}))}
                    minLength={2}
                    maxLength={120}
                    required
                />
            </label>
            <label>
                Region
                <select
                    value={form.regionCode}
                    onChange={(event) => onChange((current) => ({...current, regionCode: event.target.value}))}
                    required
                >
                    <option value="">Select region</option>
                    {regions.map((region) => (
                        <option key={region.id} value={region.code}>
                            {region.code} - {region.name}
                        </option>
                    ))}
                </select>
            </label>
        </>
    );
}

function DetailActions({isSaving}: { isSaving: boolean }) {
    return (
        <div className="action-row">
            <button type="submit" className="primary-btn" disabled={isSaving}>
                {isSaving ? "Creating..." : "Create"}
            </button>
        </div>
    );
}

function validateRegionPayload(payload: RegionPayload) {
    if (payload.name.trim().length < 2) {
        return "Region name must have at least 2 characters.";
    }

    if (payload.code.trim().length < 2) {
        return "Region code must have at least 2 characters.";
    }

    return null;
}

function validateLocalityPayload(payload: LocalityPayload) {
    if (payload.name.trim().length < 2) {
        return "County name must have at least 2 characters.";
    }

    if (!payload.regionCode) {
        return "Region is required.";
    }

    return null;
}

function cleanRegionPayload(payload: RegionPayload): RegionPayload {
    return {
        name: payload.name.trim(),
        code: payload.code.trim()
    };
}

function cleanLocalityPayload(payload: LocalityPayload): LocalityPayload {
    return {
        name: payload.name.trim(),
        regionCode: payload.regionCode.trim()
    };
}

function formatDate(value: string) {
    if (!value) {
        return "-";
    }

    return new Intl.DateTimeFormat("ro-RO", {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(new Date(value));
}

