import {type Dispatch, type FormEvent, type SetStateAction, useEffect, useMemo, useState} from "react";
import {useSearchParams} from "react-router-dom";
import {PageHeader} from "@/components/PageHeader";
import {PageFeedback} from "@/components/PageFeedback";
import {StatCard} from "@/components/StatCard";
import {Tag} from "@/components/Tag";
import {PaginationControls} from "@/components/PaginationControls";
import {useAuth} from "@/features/auth/AuthContext";
import {errorMessage} from "@/features/common/pageUtils";
import {loadAllPages} from "@/features/common/loadAllPages";
import {depotLabel as formatDepotLabel, depotLabelFromMap} from "@/features/common/displayFormatters";
import {availabilityTone} from "@/features/common/toneUtils";
import {type DepotResponse, type RegionResponse, searchDepots, searchRegions} from "@/features/users/lookupApi";
import {
    type AvailabilityStatus,
    type CourierPayload,
    type CourierResponse,
    createCourier,
    getCurrentUser,
    getDispatcherProfile,
    type PageResponse,
    searchCouriers,
    updateCourier,
    updateMyCourierAvailability
} from "@/features/users/userServiceApi";

type ActiveFilter = "" | "true" | "false";
type CourierFormValues = Required<CourierPayload>;

const PAGE_SIZE = 10;
const LOOKUP_PAGE_SIZE = 500;
const availabilityOptions: AvailabilityStatus[] = ["AVAILABLE", "OFF_DUTY", "SUSPENDED", "ON_ROUTE"];
const manualAvailabilityOptions: AvailabilityStatus[] = ["AVAILABLE", "OFF_DUTY", "SUSPENDED"];
const courierSelfOptions: AvailabilityStatus[] = ["AVAILABLE", "OFF_DUTY", "ON_ROUTE"];
const createAvailabilityOptions: AvailabilityStatus[] = ["AVAILABLE", "OFF_DUTY"];

const emptyCourierForm: CourierFormValues = {
    email: "",
    firstName: "",
    lastName: "",
    phoneNumber: "",
    depotId: "",
    regionCode: "",
    availabilityStatus: "AVAILABLE",
    active: true
};

export function CouriersPage() {
    const {user} = useAuth();

    if (user?.roles.includes("COURIER") && !user.roles.some((role) => role === "ADMIN" || role === "DISPATCHER")) {
        return <CourierSelfService/>;
    }

    return <CourierManagement/>;
}

function CourierManagement() {
    const {user} = useAuth();
    const scope = user?.roles.includes("ADMIN") ? "admin" : "dispatcher";
    const [searchParams] = useSearchParams();
    const [page, setPage] = useState<PageResponse<CourierResponse> | null>(null);
    const [filters, setFilters] = useState({
        depotId: "",
        regionCode: "",
        availabilityStatus: "",
        active: "" as ActiveFilter,
        search: ""
    });
    const [pageNumber, setPageNumber] = useState(0);
    const [selectedCourierId, setSelectedCourierId] = useState<string | null>(null);
    const [creating, setCreating] = useState(false);
    const [form, setForm] = useState<CourierFormValues>(emptyCourierForm);
    const [isLoading, setIsLoading] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [notice, setNotice] = useState<string | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);
    const [regions, setRegions] = useState<RegionResponse[]>([]);
    const [allDepots, setAllDepots] = useState<DepotResponse[]>([]);
    const [depots, setDepots] = useState<DepotResponse[]>([]);
    const [regionLookupError, setRegionLookupError] = useState<string | null>(null);
    const [depotLookupError, setDepotLookupError] = useState<string | null>(null);
    const [dispatcherProfileError, setDispatcherProfileError] = useState<string | null>(null);
    const [dispatcherProfileRegionCode, setDispatcherProfileRegionCode] = useState<string | null>(null);
    const linkedSearchTerm = searchParams.get("search")?.trim() ?? "";

    useEffect(() => {
        if (!linkedSearchTerm) {
            return;
        }

        setCreating(false);
        setPageNumber(0);
        setFilters((current) =>
            current.search === linkedSearchTerm
                ? current
                : {
                    ...current,
                    search: linkedSearchTerm
                }
        );
    }, [linkedSearchTerm]);

    useEffect(() => {
        const controller = new AbortController();
        setIsLoading(true);
        setError(null);

        searchCouriers(
            {
                page: pageNumber,
                size: linkedSearchTerm ? LOOKUP_PAGE_SIZE : PAGE_SIZE,
                depotId: filters.depotId.trim(),
                regionCode: scope === "dispatcher" ? dispatcherProfileRegionCode ?? "" : filters.regionCode.trim(),
                availabilityStatus: filters.availabilityStatus,
                active: filters.active
            },
            scope,
            controller.signal
        )
            .then(setPage)
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setError(errorMessage(loadError, "Could not load couriers."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsLoading(false);
                }
            });

        return () => controller.abort();
    }, [dispatcherProfileRegionCode, filters.active, filters.availabilityStatus, filters.depotId, filters.regionCode, pageNumber, refreshKey, scope]);

    useEffect(() => {
        const controller = new AbortController();
        setRegionLookupError(null);

        loadAllPages(
            (pageIndex, size, signal) => searchRegions({page: pageIndex, size}, signal),
            controller.signal
        )
            .then((allRegions) => {
                if (controller.signal.aborted) {
                    return;
                }

                setRegions(
                    [...allRegions].sort((first, second) =>
                        first.code.localeCompare(second.code)
                    )
                );
            })
            .catch((lookupError) => {
                if (!controller.signal.aborted) {
                    setRegionLookupError(errorMessage(lookupError, "Could not load regions."));
                }
            });

        return () => controller.abort();
    }, []);

    useEffect(() => {
        if (scope !== "dispatcher") {
            setDispatcherProfileRegionCode(null);
            setDispatcherProfileError(null);
            return;
        }

        const controller = new AbortController();
        setDispatcherProfileError(null);

        async function loadCurrentDispatcherProfile() {
            try {
                const currentUser = await getCurrentUser(controller.signal);
                const dispatcherProfileId = currentUser.dispatcherProfileId;

                if (!dispatcherProfileId) {
                    setDispatcherProfileRegionCode(null);
                    setDispatcherProfileError("The backend did not return dispatcherProfileId on /users/me for the current user.");
                    return;
                }

                const dispatcherProfile = await getDispatcherProfile(dispatcherProfileId, controller.signal);
                const regionsPage = await searchRegions({
                    code: dispatcherProfile.regionCode,
                    size: 1
                }, controller.signal);

                if (controller.signal.aborted) {
                    return;
                }

                setDispatcherProfileRegionCode(regionsPage.content[0]?.code ?? dispatcherProfile.regionCode);
            } catch (profileError) {
                if (!controller.signal.aborted) {
                    setDispatcherProfileRegionCode(null);
                    setDispatcherProfileError(errorMessage(profileError, "Could not load current dispatcher profile."));
                }
            }
        }

        void loadCurrentDispatcherProfile();

        return () => controller.abort();
    }, [scope]);

    const filteredCouriers = useMemo(
        () => (page?.content ?? []).filter((courier) => matchesProfileSearch(courier, filters.search)),
        [filters.search, page]
    );

    const selectedCourier = useMemo(
        () => page?.content.find((courier) => courier.courierProfileId === selectedCourierId) ?? null,
        [page, selectedCourierId]
    );

    const isDispatcherScoped = scope === "dispatcher";
    const dispatcherDefaults = useMemo(
        () => ({
            depotId: "",
            regionCode: dispatcherProfileRegionCode ?? ""
        }),
        [dispatcherProfileRegionCode]
    );
    const selectedRegionCode = isDispatcherScoped ? dispatcherDefaults.regionCode : form.regionCode;
    const depotsForSelectedRegion = useMemo(
        () => depots,
        [depots]
    );
    const regionIdByCode = useMemo(
        () => new Map(regions.map((region) => [region.code, region.id])),
        [regions]
    );
    const depotById = useMemo(
        () => new Map(allDepots.map((depot) => [depot.id, depot])),
        [allDepots]
    );
    const filterDepotIds = useMemo(() => {
        const ids = new Set<string>();
        allDepots.forEach((depot) => ids.add(depot.id));
        (page?.content ?? []).forEach((courier) => ids.add(courier.depotId));
        if (filters.depotId) {
            ids.add(filters.depotId);
        }
        return Array.from(ids);
    }, [allDepots, filters.depotId, page]);

    useEffect(() => {
        const controller = new AbortController();
        setDepotLookupError(null);

        loadAllPages(
            (pageIndex, size, signal) => searchDepots({page: pageIndex, size}, signal),
            controller.signal
        )
            .then((rows) => {
                if (!controller.signal.aborted) {
                    setAllDepots(rows);
                }
            })
            .catch((lookupError) => {
                if (!controller.signal.aborted) {
                    setDepotLookupError(errorMessage(lookupError, "Could not load depots."));
                }
            });

        return () => controller.abort();
    }, []);

    useEffect(() => {
        if (!selectedRegionCode || allDepots.length === 0) {
            setDepots([]);
            return;
        }

        const selectedRegionId = regionIdByCode.get(selectedRegionCode);
        if (!selectedRegionId) {
            setDepots([]);
            return;
        }

        setDepots(allDepots.filter((depot) => depot.regionId === selectedRegionId));
    }, [allDepots, regionIdByCode, selectedRegionCode]);

    useEffect(() => {
        if (creating) {
            setForm({
                ...emptyCourierForm,
                depotId: dispatcherDefaults.depotId,
                regionCode: dispatcherDefaults.regionCode
            });
            return;
        }

        if (selectedCourier) {
            setForm(toCourierForm(selectedCourier));
            return;
        }

        if (filters.search.trim()) {
            if (filteredCouriers.length) {
                setSelectedCourierId(filteredCouriers[0].courierProfileId);
                return;
            }

            setSelectedCourierId(null);
            return;
        }

        if (page?.content.length) {
            setSelectedCourierId(page.content[0].courierProfileId);
            return;
        }

        setSelectedCourierId(null);
    }, [creating, dispatcherDefaults.depotId, dispatcherDefaults.regionCode, filteredCouriers, filters.search, page, selectedCourier]);

    useEffect(() => {
        if (!creating || !isDispatcherScoped || !dispatcherDefaults.regionCode) {
            return;
        }

        setForm((current) => ({
            ...current,
            regionCode: dispatcherDefaults.regionCode
        }));
    }, [creating, dispatcherDefaults.regionCode, isDispatcherScoped]);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setNotice(null);

        try {
            const payload = isDispatcherScoped
                ? {...form, regionCode: dispatcherDefaults.regionCode}
                : form;
            const validationError = validateCourierPayload(payload, creating);

            if (validationError) {
                setError(validationError);
                return;
            }

            if (!creating && !selectedCourier) {
                setError("Select a courier before saving changes.");
                return;
            }

            setIsSaving(true);

            if (creating) {
                const created = await createCourier(payload, scope);
                setSelectedCourierId(created.courierProfileId);
                setCreating(false);
                setNotice("Courier created.");
            } else if (selectedCourier) {
                await updateCourier(selectedCourier.courierProfileId, payload, scope);
                setNotice("Courier updated.");
            }

            setRefreshKey((current) => current + 1);
        } catch (saveError) {
            setError(errorMessage(saveError, "Could not save courier."));
        } finally {
            setIsSaving(false);
        }
    }

    const availableCount = page?.content.filter((courier) => courier.availabilityStatus === "AVAILABLE").length ?? 0;
    const onRouteCount = page?.content.filter((courier) => courier.availabilityStatus === "ON_ROUTE").length ?? 0;
    const inactiveCount = page?.content.filter((courier) => !courier.user.active).length ?? 0;
    const blocksSave = !creating && selectedCourier?.availabilityStatus === "ON_ROUTE";
    const effectiveRegionCode = isDispatcherScoped ? dispatcherDefaults.regionCode : form.regionCode;
    const missingDispatcherRegion = creating && isDispatcherScoped && !effectiveRegionCode;
    const missingDepotSelection = Boolean(effectiveRegionCode) && !form.depotId;
    const dispatcherProfileLoading = isDispatcherScoped && !dispatcherDefaults.regionCode && !dispatcherProfileError;
    const errorMessages = dedupeMessages([
        error,
        regionLookupError,
        depotLookupError,
        dispatcherProfileError,
        blocksSave ? "Courier profile cannot be manually changed while ON_ROUTE." : null,
        missingDispatcherRegion ? "Dispatcher region is unavailable, so courier creation is blocked." : null,
        missingDepotSelection ? "Select a depot from the depot dropdown." : null
    ]);

    return (
        <div className="page-stack">
            <PageHeader
                title="Couriers"
                subtitle="Search, inspect, and update courier profiles using user-service permissions."
                actions={
                    <button
                        type="button"
                        className="secondary-btn"
                        disabled={dispatcherProfileLoading || Boolean(dispatcherProfileError)}
                        onClick={() => {
                            const nextDefaults = {
                                ...emptyCourierForm,
                                depotId: dispatcherDefaults.depotId,
                                regionCode: dispatcherDefaults.regionCode
                            };
                            setForm(nextDefaults);
                            setCreating(true);
                            setError(null);
                            setNotice(null);
                        }}
                    >
                        New courier
                    </button>
                }
            />

            <section className="metrics-grid">
                <StatCard label="Loaded couriers" value={String(page?.numberOfElements ?? 0)}
                          trend={`${page?.totalElements ?? 0} total`}/>
                <StatCard label="Available on page" value={String(availableCount)} tone="positive"/>
                <StatCard label="On route on page" value={String(onRouteCount)} tone="neutral"/>
                <StatCard label="Inactive on page" value={String(inactiveCount)} tone="warning"/>
            </section>

            <PageFeedback notice={notice} errors={errorMessages}/>

            <section className="two-column-grid directory-grid">
                <section className={`table-card${isLoading ? " loading" : ""}`}>
                    <div className="table-card-head">
                        <h3>Couriers</h3>
                        <p>{isLoading ? "Loading..." : `${page?.totalElements ?? 0} couriers found`}</p>
                    </div>
                    <div className="detail-block">
                        <div className="filter-grid">
                            <label>
                                Depot
                                <select
                                    value={filters.depotId}
                                    onChange={(event) => {
                                        setPageNumber(0);
                                        setFilters((current) => ({...current, depotId: event.target.value}));
                                    }}
                                >
                                    <option value="">Any depot</option>
                                    {filterDepotIds.map((depotId) => (
                                        <option key={depotId} value={depotId}>
                                            {depotLabelFromMap(depotId, depotById)}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <label>
                                Region
                                {isDispatcherScoped ? (
                                    <input value={dispatcherDefaults.regionCode} readOnly
                                           placeholder={dispatcherProfileLoading ? "Loading dispatcher region" : "Dispatcher region unavailable"}/>
                                ) : (
                                    <input
                                        value={filters.regionCode}
                                        onChange={(event) => {
                                            setPageNumber(0);
                                            setFilters((current) => ({...current, regionCode: event.target.value}));
                                        }}
                                        placeholder="RO-B"
                                    />
                                )}
                            </label>
                            <label>
                                Availability
                                <select
                                    value={filters.availabilityStatus}
                                    onChange={(event) => {
                                        setPageNumber(0);
                                        setFilters((current) => ({...current, availabilityStatus: event.target.value}));
                                    }}
                                >
                                    <option value="">Any</option>
                                    {availabilityOptions.map((status) => (
                                        <option key={status} value={status}>
                                            {status}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <label>
                                State
                                <select
                                    value={filters.active}
                                    onChange={(event) => {
                                        setPageNumber(0);
                                        setFilters((current) => ({
                                            ...current,
                                            active: event.target.value as ActiveFilter
                                        }));
                                    }}
                                >
                                    <option value="">Any</option>
                                    <option value="true">Active</option>
                                    <option value="false">Inactive</option>
                                </select>
                            </label>
                            <label>
                                Name or email
                                <input
                                    value={filters.search}
                                    onChange={(event) => setFilters((current) => ({
                                        ...current,
                                        search: event.target.value
                                    }))}
                                    placeholder="Search loaded page"
                                />
                            </label>
                        </div>
                    </div>

                    <div className="table-wrap">
                        <table>
                            <thead>
                            <tr>
                                <th>Courier</th>
                                <th>Depot</th>
                                <th>Region</th>
                                <th>Availability</th>
                                <th>State</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredCouriers.length ? (
                                filteredCouriers.map((courier) => (
                                    <tr key={courier.courierProfileId}>
                                        <td>
                                            <button
                                                type="button"
                                                className={courier.courierProfileId === selectedCourierId ? "text-link active-row" : "text-link"}
                                                onClick={() => {
                                                    setCreating(false);
                                                    setSelectedCourierId(courier.courierProfileId);
                                                }}
                                            >
                                                {fullName(courier)}
                                            </button>
                                        </td>
                                        <td>{depotLabelFromMap(courier.depotId, depotById)}</td>
                                        <td>{courier.regionCode}</td>
                                        <td><Tag text={courier.availabilityStatus}
                                                 tone={availabilityTone(courier.availabilityStatus)}/></td>
                                        <td><Tag text={courier.user.active ? "ACTIVE" : "INACTIVE"}
                                                 tone={courier.user.active ? "success" : "warning"}/></td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan={5}>No couriers match the current filters.</td>
                                </tr>
                            )}
                            </tbody>
                        </table>
                    </div>
                    {isLoading ? <div className="loading-overlay" aria-hidden="true"/> : null}

                    {page ? <PaginationControls page={page} onPageChange={setPageNumber}/> : null}
                </section>

                <section className="table-card">
                    <div className="table-card-head">
                        <h3>{creating ? "New Courier" : "Courier Details"}</h3>
                        <p>Depot assignment, user state, and availability</p>
                    </div>
                    <form className="detail-form" onSubmit={handleSubmit} noValidate>
                        <ProfileFields form={form} onChange={setForm}/>
                        <label>
                            Region
                            {isDispatcherScoped ? (
                                <input value={form.regionCode || dispatcherDefaults.regionCode} readOnly required
                                       placeholder={dispatcherProfileLoading ? "Loading dispatcher region" : "Dispatcher region unavailable"}/>
                            ) : (
                                <RegionSelectField
                                    value={form.regionCode}
                                    regions={regions}
                                    onChange={(nextRegionCode) => {
                                        setForm((current) => ({
                                            ...current,
                                            regionCode: nextRegionCode,
                                            depotId: ""
                                        }));
                                    }}
                                />
                            )}
                        </label>
                        <label>
                            Depot
                            <DepotSelectField
                                value={form.depotId}
                                depots={depotsForSelectedRegion}
                                disabled={!effectiveRegionCode}
                                onChange={(nextDepotId) => {
                                    setForm((current) => ({
                                        ...current,
                                        depotId: nextDepotId,
                                        regionCode: isDispatcherScoped ? dispatcherDefaults.regionCode : current.regionCode
                                    }));
                                }}
                            />
                        </label>
                        <label>
                            Availability
                            <select
                                value={form.availabilityStatus}
                                onChange={(event) => setForm((current) => ({
                                    ...current,
                                    availabilityStatus: event.target.value as AvailabilityStatus
                                }))}
                                disabled={blocksSave}
                            >
                                {availabilityOptionsForForm(form.availabilityStatus, creating).map((status) => (
                                    <option key={status} value={status}>
                                        {status}
                                    </option>
                                ))}
                            </select>
                        </label>
                        <label className="checkbox-label">
                            <input type="checkbox" checked={form.active} onChange={(event) => setForm((current) => ({
                                ...current,
                                active: event.target.checked
                            }))}/>
                            Active
                        </label>
                        <div className="action-row">
                            <button
                                type="submit"
                                className="primary-btn"
                                disabled={isSaving || blocksSave || dispatcherProfileLoading || Boolean(dispatcherProfileError)}
                            >
                                {isSaving ? "Saving..." : "Save courier"}
                            </button>
                        </div>
                    </form>
                </section>
            </section>
        </div>
    );
}

function CourierSelfService() {
    const {user} = useAuth();
    const [selectedAvailability, setSelectedAvailability] = useState<AvailabilityStatus>("AVAILABLE");
    const [profile, setProfile] = useState<CourierResponse | null>(null);
    const [depotById, setDepotById] = useState<Map<string, DepotResponse>>(new Map());
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [notice, setNotice] = useState<string | null>(null);

    useEffect(() => {
        const controller = new AbortController();

        loadAllPages(
            (pageIndex, size, signal) => searchDepots({page: pageIndex, size}, signal),
            controller.signal
        )
            .then((allDepots) => {
                if (!controller.signal.aborted) {
                    setDepotById(new Map(allDepots.map((depot) => [depot.id, depot])));
                }
            })
            .catch(() => undefined);

        return () => controller.abort();
    }, []);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setIsSaving(true);
        setError(null);
        setNotice(null);

        try {
            const updated = await updateMyCourierAvailability(selectedAvailability);
            setProfile(updated);
            setNotice("Availability updated.");
        } catch (saveError) {
            setError(errorMessage(saveError, "Could not update availability. The backend did not return a courier profile for this authenticated user."));
        } finally {
            setIsSaving(false);
        }
    }

    return (
        <div className="page-stack">
            <PageHeader
                title="Courier"
                subtitle="Courier profile and availability self-service."
                actions={<Tag text="Courier" tone="neutral"/>}
            />

            <PageFeedback error={error} notice={notice}/>

            <section className="two-column-grid">
                <section className="table-card">
                    <div className="table-card-head">
                        <h3>Courier Details</h3>
                        <p>Account identity and current profile data</p>
                    </div>
                    <div className="detail-block">
                        <p>
                            <strong>Name:</strong> {user?.fullName ?? "-"}
                        </p>
                        <p>
                            <strong>Email:</strong> {user?.email ?? "-"}
                        </p>
                        <p>
                            <strong>Profile:</strong> {profile?.courierProfileId ?? "Loaded after availability update"}
                        </p>
                        <p>
                            <strong>Depot:</strong> {profile?.depotId ? depotLabelFromMap(profile.depotId, depotById) : "-"}
                        </p>
                        <p>
                            <strong>Region:</strong> {profile?.regionCode ?? "-"}
                        </p>
                        <p>
                            <strong>Availability:</strong>{" "}
                            {profile ? <Tag text={profile.availabilityStatus}
                                            tone={availabilityTone(profile.availabilityStatus)}/> : "-"}
                        </p>
                    </div>
                </section>

                <section className="table-card">
                    <div className="table-card-head">
                        <h3>Availability</h3>
                        <p>Submits the selected status to user-service</p>
                    </div>
                    <form className="detail-form" onSubmit={handleSubmit}>
                        <label>
                            Status
                            <select value={selectedAvailability}
                                    onChange={(event) => setSelectedAvailability(event.target.value as AvailabilityStatus)}>
                                {courierSelfOptions.map((status) => (
                                    <option key={status} value={status}>
                                        {status}
                                    </option>
                                ))}
                            </select>
                        </label>
                        <div className="action-row">
                            <button type="submit" className="primary-btn" disabled={isSaving}>
                                {isSaving ? "Saving..." : "Update availability"}
                            </button>
                        </div>
                    </form>
                </section>
            </section>
        </div>
    );
}

function RegionSelectField({
                               value,
                               regions,
                               onChange
                           }: {
    value: string;
    regions: RegionResponse[];
    onChange: (value: string) => void;
}) {
    return (
        <select
            value={value}
            onChange={(event) => onChange(event.target.value)}
            required
        >
            <option value="">Select region</option>
            {regions.map((region) => (
                <option key={region.id} value={region.code}>
                    {region.code} - {region.name}
                </option>
            ))}
        </select>
    );
}

function DepotSelectField({
                              value,
                              depots,
                              disabled,
                              onChange
                          }: {
    value: string;
    depots: DepotResponse[];
    disabled: boolean;
    onChange: (value: string) => void;
}) {
    return (
        <select
            value={value}
            disabled={disabled}
            onChange={(event) => onChange(event.target.value)}
            required
        >
            <option value="">{disabled ? "Select region first" : "Select depot"}</option>
            {depots.map((depot) => (
                <option key={depot.id} value={depot.id}>
                    {formatDepotLabel({id: depot.id, depotCode: depot.depotCode, name: depot.name})}
                </option>
            ))}
        </select>
    );
}

function ProfileFields({
                           form,
                           onChange
                       }: {
    form: CourierFormValues;
    onChange: Dispatch<SetStateAction<CourierFormValues>>;
}) {
    return (
        <>
            <label>
                Email
                <input
                    type="email"
                    value={form.email}
                    onChange={(event) => onChange((current) => ({...current, email: event.target.value}))}
                    maxLength={255}
                    required
                />
            </label>
            <label>
                First name
                <input
                    value={form.firstName}
                    onChange={(event) => onChange((current) => ({...current, firstName: event.target.value}))}
                    minLength={2}
                    maxLength={100}
                    required
                />
            </label>
            <label>
                Last name
                <input
                    value={form.lastName}
                    onChange={(event) => onChange((current) => ({...current, lastName: event.target.value}))}
                    minLength={2}
                    maxLength={100}
                    required
                />
            </label>
            <label>
                Phone
                <input
                    value={form.phoneNumber}
                    onChange={(event) => onChange((current) => ({...current, phoneNumber: event.target.value}))}
                    maxLength={20}
                    required
                />
            </label>
        </>
    );
}

function toCourierForm(courier: CourierResponse): CourierFormValues {
    return {
        email: courier.user.email,
        firstName: courier.user.firstName,
        lastName: courier.user.lastName,
        phoneNumber: courier.user.phoneNumber,
        depotId: courier.depotId,
        regionCode: courier.regionCode,
        availabilityStatus: courier.availabilityStatus,
        active: courier.user.active
    };
}

function validateCourierPayload(payload: CourierFormValues, creating: boolean) {
    if (!payload.email.trim()) {
        return "Email is required.";
    }

    if (!payload.firstName.trim()) {
        return "First name is required.";
    }

    if (!payload.lastName.trim()) {
        return "Last name is required.";
    }

    if (!payload.phoneNumber.trim()) {
        return "Phone is required.";
    }

    if (!payload.depotId.trim()) {
        return "Select a depot from the depot dropdown.";
    }

    if (!payload.regionCode.trim()) {
        return "Region is required before saving courier.";
    }

    if (creating && !createAvailabilityOptions.includes(payload.availabilityStatus)) {
        return "New couriers can only start as AVAILABLE or OFF_DUTY.";
    }

    return null;
}

function availabilityOptionsForForm(current: AvailabilityStatus, creating: boolean) {
    const options = creating ? createAvailabilityOptions : manualAvailabilityOptions;
    return options.includes(current) ? options : [current, ...options];
}

function matchesProfileSearch(courier: CourierResponse, search: string) {
    const term = search.trim().toLowerCase();
    if (!term) {
        return true;
    }

    if (term.includes("@")) {
        return courier.user.email.toLowerCase() === term;
    }

    return `${courier.user.firstName} ${courier.user.lastName} ${courier.user.email}`.toLowerCase().includes(term);
}

function fullName(courier: CourierResponse) {
    return `${courier.user.firstName} ${courier.user.lastName}`.trim();
}

function dedupeMessages(messages: Array<string | null | undefined>) {
    return Array.from(
        new Set(
            messages
                .map((message) => message?.trim())
                .filter((message): message is string => Boolean(message))
        )
    );
}

