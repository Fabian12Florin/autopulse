import {type FormEvent, type ReactNode, useEffect, useMemo, useState} from "react";
import {PageHeader} from "@/components/PageHeader";
import {PageFeedback} from "@/components/PageFeedback";
import {Tag} from "@/components/Tag";
import {PaginationControls} from "@/components/PaginationControls";
import {useAuth} from "@/features/auth/AuthContext";
import {errorMessage} from "@/features/common/pageUtils";
import {
    activateDepot,
    createDepot,
    deactivateDepot,
    type DepotPayload,
    type DepotResponse,
    searchDepots,
    updateDepot
} from "@/features/depots/depotApi";
import {type LocalityResponse, resolveCoordinates, searchLocalities} from "@/features/geography/geographyApi";
import type {PageResponse} from "@/features/users/userServiceApi";

const PAGE_SIZE = 10;

const emptyDepotForm: DepotFormValues = {
    name: "",
    addressStreet: "",
    addressNumber: "",
    county: "",
    city: "",
    contactName: "",
    contactPhone: "",
    contactEmail: "",
    depotCode: "",
    latitude: "",
    longitude: ""
};

interface DepotFormValues {
    name: string;
    addressStreet: string;
    addressNumber: string;
    county: string;
    city: string;
    contactName: string;
    contactPhone: string;
    contactEmail: string;
    depotCode: string;
    latitude: string;
    longitude: string;
}

export function DepotsPage() {
    const {user} = useAuth();
    const isAdmin = Boolean(user?.roles.includes("ADMIN"));
    const [depotPage, setDepotPage] = useState<PageResponse<DepotResponse> | null>(null);
    const [countyOptions, setCountyOptions] = useState<LocalityResponse[]>([]);
    const [filters, setFilters] = useState({search: "", state: "" as "" | "active" | "inactive"});
    const [pageNumber, setPageNumber] = useState(0);
    const [selectedDepotId, setSelectedDepotId] = useState<string | null>(null);
    const [creating, setCreating] = useState(false);
    const [form, setForm] = useState<DepotFormValues>(emptyDepotForm);
    const [isLoading, setIsLoading] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [isResolvingCoordinates, setIsResolvingCoordinates] = useState(false);
    const [coordinatesLookupError, setCoordinatesLookupError] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [notice, setNotice] = useState<string | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        setIsLoading(true);
        setError(null);

        searchDepots({page: pageNumber, size: PAGE_SIZE, sort: "name,asc"}, controller.signal)
            .then(setDepotPage)
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setError(errorMessage(loadError, "Could not load depots."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsLoading(false);
                }
            });

        return () => controller.abort();
    }, [pageNumber, refreshKey]);

    useEffect(() => {
        const controller = new AbortController();

        searchLocalities({page: 0, size: 500}, controller.signal)
            .then((page) => setCountyOptions(page.content))
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setError(errorMessage(loadError, "Could not load county options."));
                }
            });

        return () => controller.abort();
    }, [refreshKey]);

    const filteredDepots = useMemo(() => {
        const term = filters.search.trim().toLowerCase();
        const depots = depotPage?.content ?? [];

        return depots.filter((depot) => {
            const matchesSearch = !term || [
                depot.name,
                depot.depotCode,
                depot.city ?? "",
                depot.county ?? "",
                depot.addressStreet,
                depot.contactName,
                depot.contactEmail
            ].join(" ").toLowerCase().includes(term);
            const matchesState =
                !filters.state ||
                (filters.state === "active" && depot.active) ||
                (filters.state === "inactive" && !depot.active);

            return matchesSearch && matchesState;
        });
    }, [depotPage, filters.search, filters.state]);

    const selectedDepot = useMemo(
        () => depotPage?.content.find((depot) => depot.id === selectedDepotId) ?? null,
        [depotPage, selectedDepotId]
    );

    useEffect(() => {
        setCoordinatesLookupError(null);

        if (creating) {
            setForm(emptyDepotForm);
            return;
        }

        if (selectedDepot) {
            setForm(toDepotForm(selectedDepot));
            return;
        }

        if (depotPage?.content.length) {
            setSelectedDepotId(depotPage.content[0].id);
            return;
        }

        if (depotPage) {
            setSelectedDepotId(null);
            setForm(emptyDepotForm);
        }
    }, [creating, depotPage, selectedDepot]);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setNotice(null);

        if (!isAdmin) {
            setError("Only administrators can change depot data.");
            return;
        }

        const validationError = validateDepotForm(form);
        if (validationError) {
            setError(validationError);
            return;
        }

        if (!creating && !selectedDepot) {
            setError("Select a depot before saving changes.");
            return;
        }

        try {
            setIsSaving(true);
            const payload = toDepotPayload(form);

            if (creating) {
                const created = await createDepot(payload);
                setSelectedDepotId(created.id);
                setCreating(false);
                setNotice("Depot created.");
            } else if (selectedDepot) {
                await updateDepot(selectedDepot.id, payload);
                setNotice("Depot updated.");
            }

            refresh();
        } catch (saveError) {
            setError(errorMessage(saveError, "Could not save depot."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleLifecycle() {
        if (!selectedDepot) {
            setError("Select a depot before changing its state.");
            return;
        }

        setIsSaving(true);
        setError(null);
        setNotice(null);

        try {
            const nextDepot = selectedDepot.active
                ? await deactivateDepot(selectedDepot.id)
                : await activateDepot(selectedDepot.id);
            setNotice(`${nextDepot.name} is now ${nextDepot.active ? "active" : "inactive"}.`);
            refresh();
        } catch (lifecycleError) {
            setError(errorMessage(lifecycleError, "Could not update depot state."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleResolveCoordinates() {
        setError(null);
        setNotice(null);
        setCoordinatesLookupError(null);

        const street = [form.addressStreet.trim(), form.addressNumber.trim()].filter(Boolean).join(" ").trim();
        const city = form.city.trim();
        const county = form.county.trim();

        if (!street || !city) {
            setError("Street, number, and city are required to resolve coordinates.");
            return;
        }

        try {
            setIsResolvingCoordinates(true);
            const resolved = await resolveCoordinates({
                street,
                city,
                county: county || undefined
            });
            setForm((current) => ({
                ...current,
                latitude: String(resolved.y),
                longitude: String(resolved.x)
            }));
            setNotice("Coordinates resolved from address.");
        } catch (lookupError) {
            setCoordinatesLookupError(errorMessage(lookupError, "Could not resolve coordinates from address."));
        } finally {
            setIsResolvingCoordinates(false);
        }
    }

    function refresh() {
        setRefreshKey((current) => current + 1);
    }

    return (
        <div className="page-stack">
            <PageHeader
                title="Depots"
                subtitle="Depot master data backed by the fleet-service depot endpoints."
                actions={<Tag text={isAdmin ? "Admin management" : "Read-only"}
                              tone={isAdmin ? "success" : "neutral"}/>}
            />

            <section className="toolbar-row">
                <div className="filter-row">
                    <input
                        value={filters.search}
                        onChange={(event) => setFilters((current) => ({...current, search: event.target.value}))}
                        placeholder="Search loaded page"
                    />
                    <select
                        value={filters.state}
                        onChange={(event) => setFilters((current) => ({
                            ...current,
                            state: event.target.value as "" | "active" | "inactive"
                        }))}
                    >
                        <option value="">Any state</option>
                        <option value="active">Active</option>
                        <option value="inactive">Inactive</option>
                    </select>
                </div>

                {isAdmin ? (
                    <button
                        type="button"
                        className="secondary-btn"
                        onClick={() => {
                            setCreating(true);
                            setNotice(null);
                            setError(null);
                        }}
                    >
                        New depot
                    </button>
                ) : null}
            </section>

            <PageFeedback error={error} notice={notice}/>

            <section className={isAdmin ? "two-column-grid directory-grid" : "page-stack"}>
                <DirectoryList
                    title="Depot Directory"
                    total={depotPage?.totalElements ?? 0}
                    loading={isLoading}
                    columns={["Code", "Name", "City", "County", "State", "Updated"]}
                    rows={filteredDepots.map((depot) => [
                        <button
                            key={depot.id}
                            type="button"
                            className={depot.id === selectedDepotId ? "text-link active-row" : "text-link"}
                            onClick={() => {
                                setCreating(false);
                                setSelectedDepotId(depot.id);
                            }}
                        >
                            {depot.depotCode}
                        </button>,
                        depot.name,
                        depot.city ?? "-",
                        depot.county ?? "-",
                        <Tag key={`${depot.id}-state`} text={depot.active ? "ACTIVE" : "INACTIVE"}
                             tone={depot.active ? "success" : "warning"}/>,
                        formatDate(depot.updatedAt)
                    ])}
                    emptyMessage="No depots match the current filters."
                    pagination={depotPage ? <PaginationControls page={depotPage} onPageChange={setPageNumber}/> : null}
                />

                {isAdmin ? (
                    <section className="table-card">
                        <div className="table-card-head">
                            <h3>{creating ? "New Depot" : "Depot Details"}</h3>
                            <p>Create and update payload fields required by /fleet/depots</p>
                        </div>
                        <form className="detail-form" onSubmit={handleSubmit} noValidate>
                            <DepotForm
                                form={form}
                                counties={countyOptions}
                                onChange={setForm}
                            />
                            {coordinatesLookupError ? (
                                <div className="detail-block">
                                    <p>
                                        <strong>Coordinate lookup:</strong> {coordinatesLookupError}
                                    </p>
                                </div>
                            ) : null}
                            <div className="action-row">
                                <button type="submit" className="primary-btn"
                                        disabled={isSaving || (!creating && !selectedDepot)}>
                                    {isSaving ? "Saving..." : "Save depot"}
                                </button>
                                <button type="button" className="secondary-btn" onClick={handleLifecycle}
                                        disabled={creating || !selectedDepot || isSaving}>
                                    {selectedDepot?.active ? "Deactivate" : "Activate"}
                                </button>
                                <button
                                    type="button"
                                    className="secondary-btn"
                                    onClick={() => void handleResolveCoordinates()}
                                    disabled={isResolvingCoordinates}
                                >
                                    {isResolvingCoordinates ? "Resolving..." : "Resolve Coordinates"}
                                </button>
                            </div>
                        </form>
                    </section>
                ) : null}
            </section>

            <section className="two-column-grid">
                <section className="table-card">
                    <div className="table-card-head">
                        <h3>Selected Depot</h3>
                        <p>Address, contact, coordinates, and audit values returned by backend</p>
                    </div>
                    {selectedDepot ? (
                        <div className="detail-block">
                            <p>
                                <strong>Depot:</strong> {selectedDepot.name} ({selectedDepot.depotCode})
                            </p>
                            <p>
                                <strong>Address:</strong> {selectedDepot.addressStreet} {selectedDepot.addressNumber}, {locationLabel(selectedDepot)}
                            </p>
                            <p>
                                <strong>Contact:</strong>
                            </p>
                            <ul className="detail-list">
                                <li>
                                    <strong>Name:</strong> {selectedDepot.contactName || "-"}
                                </li>
                                <li>
                                    <strong>Phone:</strong> {selectedDepot.contactPhone || "-"}
                                </li>
                                <li>
                                    <strong>Email:</strong> {selectedDepot.contactEmail || "-"}
                                </li>
                            </ul>
                            <p>
                                <strong>Coordinates:</strong> {formatCoordinate(selectedDepot.latitude)}, {formatCoordinate(selectedDepot.longitude)}
                            </p>
                            <p>
                                <strong>Created:</strong> {formatDate(selectedDepot.createdAt)}
                            </p>
                            <p>
                                <strong>Updated:</strong> {formatDate(selectedDepot.updatedAt)}
                            </p>
                        </div>
                    ) : (
                        <div className="detail-block">
                            <p>Select a depot to view details.</p>
                        </div>
                    )}
                </section>

                <section className="table-card">
                    <div className="table-card-head">
                        <h3>Depot Map</h3>
                        <p>OpenStreetMap embed centered on selected depot coordinates</p>
                    </div>
                    {selectedDepot ? (
                        <DepotMapCard selectedDepot={selectedDepot}/>
                    ) : (
                        <div className="detail-block">
                            <p>Select a depot to view the map.</p>
                        </div>
                    )}
                </section>
            </section>
        </div>
    );
}

function DirectoryList({
                           title,
                           total,
                           loading,
                           columns,
                           rows,
                           emptyMessage,
                           pagination
                       }: {
    title: string;
    total: number;
    loading: boolean;
    columns: string[];
    rows: ReactNode[][];
    emptyMessage: string;
    pagination: ReactNode;
}) {
    return (
        <section className={`table-card${loading ? " loading" : ""}`}>
            <div className="table-card-head">
                <h3>{title}</h3>
                <p>{loading ? "Loading..." : `${total} depots found`}</p>
            </div>
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
                            <tr key={`depot-row-${rowIndex}`}>
                                {row.map((cell, cellIndex) => (
                                    <td key={`depot-cell-${rowIndex}-${cellIndex}`}>{cell}</td>
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
            {loading ? <div className="loading-overlay" aria-hidden="true"/> : null}
            {pagination}
        </section>
    );
}

function DepotForm({
                       form,
                       counties,
                       onChange
                   }: {
    form: DepotFormValues;
    counties: LocalityResponse[];
    onChange: (nextForm: DepotFormValues | ((current: DepotFormValues) => DepotFormValues)) => void;
}) {
    function handleCountyChange(countyName: string) {
        onChange((current) => ({
            ...current,
            county: countyName
        }));
    }

    return (
        <>
            <div className="filter-grid">
                <label>
                    Name
                    <input
                        value={form.name}
                        onChange={(event) => onChange((current) => ({...current, name: event.target.value}))}
                        maxLength={150}
                        required
                    />
                </label>
                <label>
                    Depot code
                    <input
                        value={form.depotCode}
                        onChange={(event) => onChange((current) => ({...current, depotCode: event.target.value}))}
                        maxLength={50}
                        required
                    />
                </label>
            </div>

            <div className="filter-grid">
                <label>
                    Street
                    <input
                        value={form.addressStreet}
                        onChange={(event) => onChange((current) => ({...current, addressStreet: event.target.value}))}
                        maxLength={180}
                        required
                    />
                </label>
                <label>
                    Number
                    <input
                        value={form.addressNumber}
                        onChange={(event) => onChange((current) => ({...current, addressNumber: event.target.value}))}
                        maxLength={40}
                        required
                    />
                </label>
            </div>

            <div className="filter-grid">
                <label>
                    County
                    <select value={form.county} onChange={(event) => handleCountyChange(event.target.value)}
                            required>
                        <option value="">Select county</option>
                        {counties.map((county) => (
                            <option key={county.id} value={county.name}>
                                {county.name} ({county.region.code})
                            </option>
                        ))}
                    </select>
                </label>
                <label>
                    City
                    <input
                        value={form.city}
                        onChange={(event) => onChange((current) => ({...current, city: event.target.value}))}
                        maxLength={150}
                        required
                    />
                </label>
            </div>

            <div className="filter-grid">
                <label>
                    Latitude
                    <input
                        type="number"
                        step="any"
                        value={form.latitude}
                        onChange={(event) => onChange((current) => ({...current, latitude: event.target.value}))}
                        required
                    />
                </label>
                <label>
                    Longitude
                    <input
                        type="number"
                        step="any"
                        value={form.longitude}
                        onChange={(event) => onChange((current) => ({...current, longitude: event.target.value}))}
                        required
                    />
                </label>
            </div>

            <div className="filter-grid">
                <label>
                    Contact name
                    <input
                        value={form.contactName}
                        onChange={(event) => onChange((current) => ({...current, contactName: event.target.value}))}
                        maxLength={120}
                        required
                    />
                </label>
                <label>
                    Contact phone
                    <input
                        value={form.contactPhone}
                        onChange={(event) => onChange((current) => ({...current, contactPhone: event.target.value}))}
                        maxLength={40}
                        required
                    />
                </label>
                <label>
                    Contact email
                    <input
                        type="email"
                        value={form.contactEmail}
                        onChange={(event) => onChange((current) => ({...current, contactEmail: event.target.value}))}
                        maxLength={180}
                        required
                    />
                </label>
            </div>
        </>
    );
}

function toDepotForm(depot: DepotResponse): DepotFormValues {
    return {
        name: depot.name ?? "",
        addressStreet: depot.addressStreet ?? "",
        addressNumber: depot.addressNumber ?? "",
        county: depot.county ?? "",
        city: depot.city ?? "",
        contactName: depot.contactName ?? "",
        contactPhone: depot.contactPhone ?? "",
        contactEmail: depot.contactEmail ?? "",
        depotCode: depot.depotCode ?? "",
        latitude: depot.latitude === null ? "" : String(depot.latitude),
        longitude: depot.longitude === null ? "" : String(depot.longitude)
    };
}

function toDepotPayload(form: DepotFormValues): DepotPayload {
    return {
        name: form.name.trim(),
        addressStreet: form.addressStreet.trim(),
        addressNumber: form.addressNumber.trim(),
        county: form.county.trim(),
        city: form.city.trim(),
        contactName: form.contactName.trim(),
        contactPhone: form.contactPhone.trim(),
        contactEmail: form.contactEmail.trim(),
        depotCode: form.depotCode.trim(),
        latitude: Number(form.latitude),
        longitude: Number(form.longitude)
    };
}

function validateDepotForm(form: DepotFormValues) {
    if (!form.name.trim()) {
        return "Depot name is required.";
    }

    if (!form.addressStreet.trim()) {
        return "Street is required.";
    }

    if (!form.addressNumber.trim()) {
        return "Address number is required.";
    }

    if (!form.county.trim()) {
        return "County is required.";
    }

    if (!form.city.trim()) {
        return "City is required.";
    }

    if (!form.contactName.trim()) {
        return "Contact name is required.";
    }

    if (!form.contactPhone.trim()) {
        return "Contact phone is required.";
    }

    if (!form.contactEmail.trim() || !form.contactEmail.includes("@")) {
        return "A valid contact email is required.";
    }

    if (!form.depotCode.trim()) {
        return "Depot code is required.";
    }

    if (!Number.isFinite(Number(form.latitude))) {
        return "Latitude must be a valid number.";
    }

    if (!Number.isFinite(Number(form.longitude))) {
        return "Longitude must be a valid number.";
    }

    return null;
}

function locationLabel(depot: DepotResponse) {
    const city = (depot.city ?? "").trim();
    const county = (depot.county ?? "").trim();
    if (city && county) {
        return `${city}, ${county}`;
    }
    if (city) {
        return city;
    }
    if (county) {
        return county;
    }
    return "-";
}

function formatCoordinate(value: number | null | undefined) {
    return typeof value === "number" ? value.toFixed(6) : "-";
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

function DepotMapCard({
                          selectedDepot
                      }: {
    selectedDepot: DepotResponse;
}) {
    const latitude = selectedDepot.latitude;
    const longitude = selectedDepot.longitude;

    if (latitude === null || longitude === null) {
        return (
            <div className="detail-block">
                <p>No coordinates available for this depot.</p>
            </div>
        );
    }

    const mapUrl = buildOpenStreetMapEmbedUrl(latitude, longitude);
    const openMapUrl = buildOpenStreetMapLink(latitude, longitude);

    return (
        <div className="detail-block route-map-block">
            <p>
                <strong>Map:</strong>{" "}
                <a className="route-map-link" href={openMapUrl} target="_blank" rel="noreferrer">
                    Open in OpenStreetMap
                </a>
            </p>
            <iframe
                className="route-map-frame depot-map-frame"
                src={mapUrl}
                title={`Depot map ${selectedDepot.depotCode}`}
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
            />
        </div>
    );
}

function buildOpenStreetMapEmbedUrl(latitude: number, longitude: number) {
    const bboxOffset = 0.03;
    const bbox = [
        longitude - bboxOffset,
        latitude - bboxOffset,
        longitude + bboxOffset,
        latitude + bboxOffset
    ]
        .map((value) => value.toFixed(6))
        .join(",");

    const marker = `${latitude.toFixed(6)},${longitude.toFixed(6)}`;
    return `https://www.openstreetmap.org/export/embed.html?bbox=${encodeURIComponent(bbox)}&layer=mapnik&marker=${encodeURIComponent(marker)}`;
}

function buildOpenStreetMapLink(latitude: number, longitude: number) {
    const lat = latitude.toFixed(6);
    const lon = longitude.toFixed(6);
    return `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=13/${lat}/${lon}`;
}

