import {type FormEvent, type ReactNode, useEffect, useMemo, useState} from "react";
import {Link} from "react-router-dom";
import {PaginationControls} from "@/components/PaginationControls";
import {PageFeedback} from "@/components/PageFeedback";
import {PageHeader} from "@/components/PageHeader";
import {Tag} from "@/components/Tag";
import {useAuth} from "@/features/auth/AuthContext";
import {loadAllPages} from "@/features/common/loadAllPages";
import {courierLabel as formatCourierLabel, depotLabel as formatDepotLabel} from "@/features/common/displayFormatters";
import {errorMessage} from "@/features/common/pageUtils";
import {type DepotResponse, searchDepots} from "@/features/depots/depotApi";
import {
    activateVehicle,
    assignVehicle,
    createVehicle,
    createVehicleDocument,
    createVehicleMaintenanceRecord,
    deactivateVehicle,
    deleteVehicleDocument,
    deleteVehicleMaintenanceRecord,
    endVehicleAssignment,
    type FuelType,
    searchVehicleAssignments,
    searchVehicleDocuments,
    searchVehicleMaintenanceRecords,
    searchVehicles,
    updateVehicle,
    updateVehicleDocument,
    updateVehicleMaintenanceRecord,
    updateVehicleStatus,
    type VehicleAssignmentResponse,
    type VehicleCategory,
    type VehicleDocumentPayload,
    type VehicleDocumentResponse,
    type VehicleDocumentType,
    type VehicleMaintenancePayload,
    type VehicleMaintenanceResponse,
    type VehiclePayload,
    type VehicleResponse,
    type VehicleStatus
} from "@/features/fleet/fleetApi";
import {type CourierResponse, type PageResponse, searchCouriers} from "@/features/users/userServiceApi";

const PAGE_SIZE = 10;
const LOOKUP_PAGE_SIZE = 500;

const categoryOptions: VehicleCategory[] = ["M1", "M2", "M3"];
const fuelTypeOptions: FuelType[] = ["PETROL", "DIESEL", "ELECTRIC", "HYBRID", "LPG"];
const statusOptions: VehicleStatus[] = ["AVAILABLE", "IN_USE", "MAINTENANCE", "OUT_OF_SERVICE"];
const documentTypeOptions: VehicleDocumentType[] = ["INSURANCE", "VIGNETTE", "PERIODIC_INSPECTION"];

type StatusFilter = "" | VehicleStatus;

interface VehicleFormValues {
    depotId: string;
    licensePlate: string;
    brand: string;
    model: string;
    vin: string;
    year: string;
    category: VehicleCategory;
    fuelType: FuelType;
    capacityWeight: string;
    capacityVolume: string;
}

const emptyVehicleForm: VehicleFormValues = {
    depotId: "",
    licensePlate: "",
    brand: "",
    model: "",
    vin: "",
    year: "",
    category: "M1",
    fuelType: "DIESEL",
    capacityWeight: "",
    capacityVolume: ""
};

interface MaintenanceFormValues {
    mileage: string;
    serviceDate: string;
    cost: string;
    description: string;
    serviceProvider: string;
}

interface DocumentFormValues {
    documentType: VehicleDocumentType;
    issuedAt: string;
    expiresAt: string;
    cost: string;
    description: string;
}

const emptyMaintenanceForm: MaintenanceFormValues = {
    mileage: "",
    serviceDate: "",
    cost: "",
    description: "",
    serviceProvider: ""
};

const emptyDocumentForm: DocumentFormValues = {
    documentType: "INSURANCE",
    issuedAt: "",
    expiresAt: "",
    cost: "",
    description: ""
};

export function FleetPage() {
    const {user} = useAuth();
    const isAdmin = Boolean(user?.roles.includes("ADMIN"));
    const courierScope = isAdmin ? "admin" : "dispatcher";
    const [vehiclePage, setVehiclePage] = useState<PageResponse<VehicleResponse> | null>(null);
    const [assignments, setAssignments] = useState<VehicleAssignmentResponse[]>([]);
    const [depots, setDepots] = useState<DepotResponse[]>([]);
    const [couriers, setCouriers] = useState<CourierResponse[]>([]);
    const [filters, setFilters] = useState({search: "", status: "" as StatusFilter});
    const [pageNumber, setPageNumber] = useState(0);
    const [selectedVehicleId, setSelectedVehicleId] = useState<string | null>(null);
    const [creating, setCreating] = useState(false);
    const [form, setForm] = useState<VehicleFormValues>(emptyVehicleForm);
    const [assignmentCourierId, setAssignmentCourierId] = useState("");
    const [maintenanceRecords, setMaintenanceRecords] = useState<VehicleMaintenanceResponse[]>([]);
    const [documents, setDocuments] = useState<VehicleDocumentResponse[]>([]);
    const [selectedMaintenanceId, setSelectedMaintenanceId] = useState<string | null>(null);
    const [selectedDocumentId, setSelectedDocumentId] = useState<string | null>(null);
    const [maintenanceForm, setMaintenanceForm] = useState<MaintenanceFormValues>(emptyMaintenanceForm);
    const [documentForm, setDocumentForm] = useState<DocumentFormValues>(emptyDocumentForm);
    const [isLoading, setIsLoading] = useState(false);
    const [isVehicleDetailLoading, setIsVehicleDetailLoading] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [notice, setNotice] = useState<string | null>(null);
    const [lookupError, setLookupError] = useState<string | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        setIsLoading(true);
        setError(null);

        Promise.all([
            searchVehicles({page: pageNumber, size: PAGE_SIZE, sort: "licensePlate,asc"}, controller.signal),
            searchVehicleAssignments({page: 0, size: LOOKUP_PAGE_SIZE, sort: "assignedAt,desc"}, controller.signal)
        ])
            .then(([vehicles, assignmentPage]) => {
                if (controller.signal.aborted) {
                    return;
                }

                setVehiclePage(vehicles);
                setAssignments(assignmentPage.content ?? []);
            })
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setError(errorMessage(loadError, "Could not load fleet data."));
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
        setLookupError(null);

        Promise.all([
            loadAllPages((pageIndex, size, signal) => searchDepots({
                page: pageIndex,
                size,
                sort: "name,asc"
            }, signal), controller.signal),
            loadAllPages((pageIndex, size, signal) => searchCouriers({
                page: pageIndex,
                size,
                active: "true"
            }, courierScope, signal), controller.signal)
        ])
            .then(([depotRows, courierRows]) => {
                if (controller.signal.aborted) {
                    return;
                }

                setDepots(depotRows);
                setCouriers(courierRows);
            })
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setLookupError(errorMessage(loadError, "Could not load depot or courier options."));
                }
            });

        return () => controller.abort();
    }, [courierScope, refreshKey]);

    const activeAssignmentByVehicleId = useMemo(
        () => new Map(
            assignments
                .filter((assignment) => assignment.status === "ACTIVE")
                .map((assignment) => [assignment.vehicleId, assignment])
        ),
        [assignments]
    );

    const depotById = useMemo(
        () => new Map(depots.map((depot) => [depot.id, depot])),
        [depots]
    );

    const courierById = useMemo(
        () => new Map(couriers.map((courier) => [courier.courierProfileId, courier])),
        [couriers]
    );

    const filteredVehicles = useMemo(() => {
        const term = filters.search.trim().toLowerCase();
        const vehicles = vehiclePage?.content ?? [];

        return vehicles.filter((vehicle) => {
            const depot = depotById.get(vehicle.depotId);
            const matchesSearch = !term || [
                vehicle.licensePlate,
                vehicle.brand,
                vehicle.model,
                vehicle.vin,
                depot?.name ?? "",
                depot?.depotCode ?? ""
            ].join(" ").toLowerCase().includes(term);
            const matchesStatus = !filters.status || vehicle.status === filters.status;

            return matchesSearch && matchesStatus;
        });
    }, [depotById, filters.search, filters.status, vehiclePage]);

    const selectedVehicle = useMemo(
        () => vehiclePage?.content.find((vehicle) => vehicle.id === selectedVehicleId) ?? null,
        [selectedVehicleId, vehiclePage]
    );

    const selectedAssignment = selectedVehicle ? activeAssignmentByVehicleId.get(selectedVehicle.id) ?? null : null;
    const selectedDepot = selectedVehicle ? depotById.get(selectedVehicle.depotId) ?? null : null;
    const selectedCourier = selectedAssignment ? courierById.get(selectedAssignment.courierId) ?? null : null;
    const selectedDepotIdForAssignment = creating ? form.depotId : selectedVehicle?.depotId;
    const couriersForSelectedDepot = couriers.filter((courier) => courier.depotId === selectedDepotIdForAssignment);
    const selectedMaintenance = maintenanceRecords.find((record) => record.id === selectedMaintenanceId) ?? null;
    const selectedDocument = documents.find((document) => document.id === selectedDocumentId) ?? null;

    useEffect(() => {
        setAssignmentCourierId("");

        if (creating) {
            setForm(emptyVehicleForm);
            return;
        }

        if (selectedVehicle) {
            setForm(toVehicleForm(selectedVehicle));
            return;
        }

        if (vehiclePage?.content.length) {
            setSelectedVehicleId(vehiclePage.content[0].id);
            return;
        }

        if (vehiclePage) {
            setSelectedVehicleId(null);
            setForm(emptyVehicleForm);
        }
    }, [creating, selectedVehicle, vehiclePage]);

    useEffect(() => {
        setMaintenanceRecords([]);
        setDocuments([]);
        setSelectedMaintenanceId(null);
        setSelectedDocumentId(null);
        setMaintenanceForm(emptyMaintenanceForm);
        setDocumentForm(emptyDocumentForm);

        if (!selectedVehicle) {
            return;
        }

        const controller = new AbortController();
        setIsVehicleDetailLoading(true);

        Promise.all([
            searchVehicleMaintenanceRecords(selectedVehicle.id, {
                page: 0,
                size: LOOKUP_PAGE_SIZE,
                sort: "serviceDate,desc"
            }, controller.signal),
            searchVehicleDocuments(selectedVehicle.id, {
                page: 0,
                size: LOOKUP_PAGE_SIZE,
                sort: "expiresAt,asc"
            }, controller.signal)
        ])
            .then(([maintenancePage, documentPage]) => {
                if (controller.signal.aborted) {
                    return;
                }

                const maintenanceRows = maintenancePage.content ?? [];
                const documentRows = documentPage.content ?? [];
                setMaintenanceRecords(maintenanceRows);
                setDocuments(documentRows);

                if (maintenanceRows[0]) {
                    setSelectedMaintenanceId(maintenanceRows[0].id);
                    setMaintenanceForm(toMaintenanceForm(maintenanceRows[0]));
                }

                if (documentRows[0]) {
                    setSelectedDocumentId(documentRows[0].id);
                    setDocumentForm(toDocumentForm(documentRows[0]));
                }
            })
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setError(errorMessage(loadError, "Could not load vehicle maintenance or document records."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsVehicleDetailLoading(false);
                }
            });

        return () => controller.abort();
    }, [selectedVehicle]);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setNotice(null);

        if (!isAdmin) {
            setError("Only administrators can create or update vehicles.");
            return;
        }

        const validationError = validateVehicleForm(form);
        if (validationError) {
            setError(validationError);
            return;
        }

        if (!creating && !selectedVehicle) {
            setError("Select a vehicle before saving changes.");
            return;
        }

        try {
            setIsSaving(true);
            const payload = toVehiclePayload(form);

            if (creating) {
                const created = await createVehicle(payload);
                setSelectedVehicleId(created.id);
                setCreating(false);
                setNotice("Vehicle created.");
            } else if (selectedVehicle) {
                await updateVehicle(selectedVehicle.id, payload);
                setNotice("Vehicle updated.");
            }

            refresh();
        } catch (saveError) {
            setError(errorMessage(saveError, "Could not save vehicle."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleStatusChange(status: VehicleStatus) {
        if (!selectedVehicle) {
            setError("Select a vehicle before changing its status.");
            return;
        }

        setError(null);
        setNotice(null);

        try {
            setIsSaving(true);
            await updateVehicleStatus(selectedVehicle.id, status);
            setNotice(`Vehicle status changed to ${status}.`);
            refresh();
        } catch (statusError) {
            setError(errorMessage(statusError, "Could not update vehicle status."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleLifecycle() {
        if (!selectedVehicle) {
            setError("Select a vehicle before changing lifecycle state.");
            return;
        }

        setError(null);
        setNotice(null);

        try {
            setIsSaving(true);
            const nextVehicle = selectedVehicle.status === "OUT_OF_SERVICE"
                ? await activateVehicle(selectedVehicle.id)
                : await deactivateVehicle(selectedVehicle.id);
            setNotice(`${nextVehicle.licensePlate} is now ${nextVehicle.status}.`);
            refresh();
        } catch (lifecycleError) {
            setError(errorMessage(lifecycleError, "Could not update vehicle lifecycle state."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleAssignVehicle() {
        if (!selectedVehicle) {
            setError("Select a vehicle before assigning it.");
            return;
        }

        if (selectedAssignment) {
            setError("End the current assignment before assigning this vehicle again.");
            return;
        }

        if (selectedVehicle.status !== "AVAILABLE") {
            setError("Backend only accepts assignments for AVAILABLE vehicles.");
            return;
        }

        if (!assignmentCourierId) {
            setError("Select a courier before assigning the vehicle.");
            return;
        }

        setError(null);
        setNotice(null);

        try {
            setIsSaving(true);
            await assignVehicle(selectedVehicle.id, assignmentCourierId);
            setNotice("Vehicle assigned.");
            refresh();
        } catch (assignmentError) {
            setError(errorMessage(assignmentError, "Could not assign vehicle."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleEndAssignment() {
        if (!selectedAssignment) {
            setError("This vehicle does not have an active assignment.");
            return;
        }

        setError(null);
        setNotice(null);

        try {
            setIsSaving(true);
            await endVehicleAssignment(selectedAssignment.id);
            setNotice("Vehicle assignment ended.");
            refresh();
        } catch (assignmentError) {
            setError(errorMessage(assignmentError, "Could not end vehicle assignment."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleMaintenanceSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setNotice(null);

        if (!selectedVehicle) {
            setError("Select a vehicle before saving maintenance records.");
            return;
        }

        if (!isAdmin) {
            setError("Only administrators can change maintenance records.");
            return;
        }

        const validationError = validateMaintenanceForm(maintenanceForm);
        if (validationError) {
            setError(validationError);
            return;
        }

        try {
            setIsSaving(true);
            const payload = toMaintenancePayload(maintenanceForm);

            if (selectedMaintenance) {
                await updateVehicleMaintenanceRecord(selectedMaintenance.id, payload);
                setNotice("Maintenance record updated.");
            } else {
                const created = await createVehicleMaintenanceRecord(selectedVehicle.id, payload);
                setSelectedMaintenanceId(created.id);
                setNotice("Maintenance record created.");
            }

            await reloadVehicleDetails(selectedVehicle.id);
        } catch (saveError) {
            setError(errorMessage(saveError, "Could not save maintenance record."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleDeleteMaintenance() {
        if (!selectedMaintenance) {
            setError("Select a maintenance record before deleting it.");
            return;
        }

        if (!isAdmin) {
            setError("Only administrators can delete maintenance records.");
            return;
        }

        setError(null);
        setNotice(null);

        try {
            setIsSaving(true);
            await deleteVehicleMaintenanceRecord(selectedMaintenance.id);
            setSelectedMaintenanceId(null);
            setMaintenanceForm(emptyMaintenanceForm);
            setNotice("Maintenance record deleted.");

            if (selectedVehicle) {
                await reloadVehicleDetails(selectedVehicle.id);
            }
        } catch (deleteError) {
            setError(errorMessage(deleteError, "Could not delete maintenance record."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleDocumentSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setNotice(null);

        if (!selectedVehicle) {
            setError("Select a vehicle before saving documents.");
            return;
        }

        if (!isAdmin) {
            setError("Only administrators can change vehicle documents.");
            return;
        }

        const validationError = validateDocumentForm(documentForm);
        if (validationError) {
            setError(validationError);
            return;
        }

        try {
            setIsSaving(true);
            const payload = toDocumentPayload(documentForm);

            if (selectedDocument) {
                await updateVehicleDocument(selectedDocument.id, payload);
                setNotice("Vehicle document updated.");
            } else {
                const created = await createVehicleDocument(selectedVehicle.id, payload);
                setSelectedDocumentId(created.id);
                setNotice("Vehicle document created.");
            }

            await reloadVehicleDetails(selectedVehicle.id);
        } catch (saveError) {
            setError(errorMessage(saveError, "Could not save vehicle document."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleDeleteDocument() {
        if (!selectedDocument) {
            setError("Select a vehicle document before deleting it.");
            return;
        }

        if (!isAdmin) {
            setError("Only administrators can delete vehicle documents.");
            return;
        }

        setError(null);
        setNotice(null);

        try {
            setIsSaving(true);
            await deleteVehicleDocument(selectedDocument.id);
            setSelectedDocumentId(null);
            setDocumentForm(emptyDocumentForm);
            setNotice("Vehicle document deleted.");

            if (selectedVehicle) {
                await reloadVehicleDetails(selectedVehicle.id);
            }
        } catch (deleteError) {
            setError(errorMessage(deleteError, "Could not delete vehicle document."));
        } finally {
            setIsSaving(false);
        }
    }

    async function reloadVehicleDetails(vehicleId: string) {
        const [maintenancePage, documentPage] = await Promise.all([
            searchVehicleMaintenanceRecords(vehicleId, {page: 0, size: LOOKUP_PAGE_SIZE, sort: "serviceDate,desc"}),
            searchVehicleDocuments(vehicleId, {page: 0, size: LOOKUP_PAGE_SIZE, sort: "expiresAt,asc"})
        ]);
        const maintenanceRows = maintenancePage.content ?? [];
        const documentRows = documentPage.content ?? [];
        setMaintenanceRecords(maintenanceRows);
        setDocuments(documentRows);

        if (selectedMaintenanceId && !maintenanceRows.some((record) => record.id === selectedMaintenanceId)) {
            setSelectedMaintenanceId(null);
            setMaintenanceForm(emptyMaintenanceForm);
        }

        if (selectedDocumentId && !documentRows.some((document) => document.id === selectedDocumentId)) {
            setSelectedDocumentId(null);
            setDocumentForm(emptyDocumentForm);
        }
    }

    function refresh() {
        setRefreshKey((current) => current + 1);
    }

    return (
        <div className="page-stack">
            <PageHeader
                title="Fleet"
                subtitle="Vehicle inventory, status changes, and assignment flows backed by fleet-service."
                actions={<Tag text={isAdmin ? "Admin management" : "Dispatcher operations"}
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
                        value={filters.status}
                        onChange={(event) => setFilters((current) => ({
                            ...current,
                            status: event.target.value as StatusFilter
                        }))}
                    >
                        <option value="">Any status</option>
                        {statusOptions.map((status) => (
                            <option key={status} value={status}>{status}</option>
                        ))}
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
                        New vehicle
                    </button>
                ) : null}
            </section>

            <PageFeedback error={error} notice={notice} errors={lookupError ? [lookupError] : []}/>

            <section className={isAdmin ? "two-column-grid directory-grid" : "page-stack"}>
                <DirectoryList
                    title="Vehicle Inventory"
                    total={vehiclePage?.totalElements ?? 0}
                    loading={isLoading}
                    columns={["Plate", "Depot", "Vehicle", "Capacity", "Status", "Assignment"]}
                    rows={filteredVehicles.map((vehicle) => {
                        const activeAssignment = activeAssignmentByVehicleId.get(vehicle.id);
                        return [
                            <button
                                key={vehicle.id}
                                type="button"
                                className={vehicle.id === selectedVehicleId ? "text-link active-row" : "text-link"}
                                onClick={() => {
                                    setCreating(false);
                                    setSelectedVehicleId(vehicle.id);
                                }}
                            >
                                {vehicle.licensePlate}
                            </button>,
                            depotLabel(depotById.get(vehicle.depotId)),
                            `${vehicle.brand} ${vehicle.model}`,
                            `${formatNumber(vehicle.capacityWeight)} kg / ${formatNumber(vehicle.capacityVolume)} m3`,
                            <Tag key={`${vehicle.id}-status`} text={vehicle.status} tone={statusTone(vehicle.status)}/>,
                            activeAssignment ? courierLabel(courierById.get(activeAssignment.courierId), activeAssignment.courierId) : "-"
                        ];
                    })}
                    emptyMessage="No vehicles match the current filters."
                    pagination={vehiclePage ?
                        <PaginationControls page={vehiclePage} onPageChange={setPageNumber}/> : null}
                />

                {isAdmin ? (
                    <section className="table-card">
                        <div className="table-card-head">
                            <h3>{creating ? "New Vehicle" : "Vehicle Payload"}</h3>
                            <p>Create and update fields required by /fleet/vehicles</p>
                        </div>
                        <form className="detail-form" onSubmit={handleSubmit} noValidate>
                            <VehicleForm form={form} depots={depots} onChange={setForm}/>
                            <div className="action-row">
                                <button type="submit" className="primary-btn"
                                        disabled={isSaving || (!creating && !selectedVehicle)}>
                                    {isSaving ? "Saving..." : "Save vehicle"}
                                </button>
                                <button type="button" className="secondary-btn" onClick={handleLifecycle}
                                        disabled={creating || !selectedVehicle || isSaving}>
                                    {selectedVehicle?.status === "OUT_OF_SERVICE" ? "Activate" : "Deactivate"}
                                </button>
                                {creating ? (
                                    <button type="button" className="secondary-btn" onClick={() => setCreating(false)}>
                                        Cancel
                                    </button>
                                ) : null}
                            </div>
                        </form>
                    </section>
                ) : null}
            </section>

            <section className="two-column-grid directory-grid">
                <section className="table-card">
                    <div className="table-card-head">
                        <h3>Selected Vehicle</h3>
                        <p>Response fields returned by fleet-service</p>
                    </div>
                    {selectedVehicle ? (
                        <div className="detail-block">
                            <p>
                                <strong>Vehicle:</strong> {selectedVehicle.licensePlate} ({selectedVehicle.brand} {selectedVehicle.model})
                            </p>
                            <p>
                                <strong>VIN / Year:</strong> {selectedVehicle.vin} / {selectedVehicle.year}
                            </p>
                            <p>
                                <strong>Depot:</strong> {depotLabel(selectedDepot)}
                            </p>
                            <p>
                                <strong>Category /
                                    fuel:</strong> {selectedVehicle.category} / {selectedVehicle.fuelType}
                            </p>
                            <p>
                                <strong>Capacity:</strong> {formatNumber(selectedVehicle.capacityWeight)} kg, {formatNumber(selectedVehicle.capacityVolume)} m3
                            </p>
                            <p>
                                <strong>Status:</strong> {selectedVehicle.status}
                            </p>
                            <p>
                                <strong>Created:</strong> {formatDate(selectedVehicle.createdAt)}
                            </p>
                            <p>
                                <strong>Updated:</strong> {formatDate(selectedVehicle.updatedAt)}
                            </p>
                            <div className="action-row">
                                {statusOptions.map((status) => (
                                    <button
                                        key={status}
                                        className="secondary-btn"
                                        type="button"
                                        onClick={() => handleStatusChange(status)}
                                        disabled={isSaving || selectedVehicle.status === status}
                                    >
                                        {status}
                                    </button>
                                ))}
                            </div>
                        </div>
                    ) : (
                        <div className="detail-block">
                            <p>Select a vehicle to inspect details.</p>
                        </div>
                    )}
                </section>

                <section className="table-card">
                    <div className="table-card-head">
                        <h3>Vehicle Assignment</h3>
                        <p>Uses /fleet/vehicle-assignments and active courier assignments</p>
                    </div>
                    {selectedVehicle ? (
                        <div className="detail-block">
                            <p>
                                <strong>Active
                                    assignment:</strong> {selectedAssignment ? courierLabel(selectedCourier, selectedAssignment.courierId) : "None"}
                            </p>
                            {selectedAssignment ? (
                                <>
                                    <p>
                                        <strong>Assigned at:</strong> {formatDate(selectedAssignment.assignedAt)}
                                    </p>
                                    <p>
                                        <strong>Courier phone:</strong> {selectedCourier?.user.phoneNumber || "-"}
                                    </p>
                                    <p>
                                        <strong>Courier email:</strong> {selectedCourier?.user.email || "-"}
                                    </p>
                                    <p>
                                        <strong>Profile:</strong>{" "}
                                        <Link
                                            className="text-link"
                                            to={courierProfileLink(selectedCourier, selectedAssignment.courierId)}
                                        >
                                            View profile
                                        </Link>
                                    </p>
                                    <div className="action-row">
                                        <button type="button" className="secondary-btn" onClick={handleEndAssignment}
                                                disabled={isSaving}>
                                            End assignment
                                        </button>
                                    </div>
                                </>
                            ) : (
                                <div className="filter-row">
                                    <select
                                        className="role-select"
                                        value={assignmentCourierId}
                                        onChange={(event) => setAssignmentCourierId(event.target.value)}
                                    >
                                        <option value="">Select courier</option>
                                        {couriersForSelectedDepot.map((courier) => (
                                            <option key={courier.courierProfileId} value={courier.courierProfileId}>
                                                {courierLabel(courier, courier.courierProfileId)} - {courier.availabilityStatus}
                                            </option>
                                        ))}
                                    </select>
                                    <button
                                        className="secondary-btn"
                                        type="button"
                                        onClick={handleAssignVehicle}
                                        disabled={isSaving || selectedVehicle.status !== "AVAILABLE"}
                                    >
                                        Assign
                                    </button>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="detail-block">
                            <p>Select a vehicle before managing assignments.</p>
                        </div>
                    )}
                </section>
            </section>

            <section className="two-column-grid directory-grid">
                <section className="table-card">
                    <div className="table-card-head">
                        <h3>Maintenance History</h3>
                        <p>{isVehicleDetailLoading ? "Loading..." : `${maintenanceRecords.length} records for selected vehicle`}</p>
                    </div>
                    {selectedVehicle ? (
                        <>
                            <SimpleTable
                                columns={["Date", "Mileage", "Cost", "Provider"]}
                                rows={maintenanceRecords.map((record) => [
                                    <button
                                        key={record.id}
                                        type="button"
                                        className={record.id === selectedMaintenanceId ? "text-link active-row" : "text-link"}
                                        onClick={() => {
                                            setSelectedMaintenanceId(record.id);
                                            setMaintenanceForm(toMaintenanceForm(record));
                                        }}
                                    >
                                        {formatDateOnly(record.serviceDate)}
                                    </button>,
                                    `${formatNumber(record.mileage)} km`,
                                    formatCurrency(record.cost),
                                    record.serviceProvider ?? "-"
                                ])}
                                emptyMessage="No maintenance records for this vehicle."
                            />
                            {isAdmin ? (
                                <form className="detail-form" onSubmit={handleMaintenanceSubmit} noValidate>
                                    <MaintenanceForm form={maintenanceForm} onChange={setMaintenanceForm}/>
                                    <div className="action-row">
                                        <button type="submit" className="primary-btn" disabled={isSaving}>
                                            {selectedMaintenance ? "Update record" : "Add record"}
                                        </button>
                                        <button
                                            type="button"
                                            className="secondary-btn"
                                            onClick={() => {
                                                setSelectedMaintenanceId(null);
                                                setMaintenanceForm(emptyMaintenanceForm);
                                            }}
                                        >
                                            New record
                                        </button>
                                        <button
                                            type="button"
                                            className="secondary-btn"
                                            onClick={handleDeleteMaintenance}
                                            disabled={!selectedMaintenance || isSaving}
                                        >
                                            Delete
                                        </button>
                                    </div>
                                </form>
                            ) : null}
                        </>
                    ) : (
                        <div className="detail-block">
                            <p>Select a vehicle to view maintenance history.</p>
                        </div>
                    )}
                </section>

                <section className="table-card">
                    <div className="table-card-head">
                        <h3>Vehicle Documents</h3>
                        <p>{isVehicleDetailLoading ? "Loading..." : `${documents.length} documents for selected vehicle`}</p>
                    </div>
                    {selectedVehicle ? (
                        <>
                            <SimpleTable
                                columns={["Type", "Issued", "Expires", "Cost"]}
                                rows={documents.map((document) => [
                                    <button
                                        key={document.id}
                                        type="button"
                                        className={document.id === selectedDocumentId ? "text-link active-row" : "text-link"}
                                        onClick={() => {
                                            setSelectedDocumentId(document.id);
                                            setDocumentForm(toDocumentForm(document));
                                        }}
                                    >
                                        {document.documentType}
                                    </button>,
                                    formatDateOnly(document.issuedAt),
                                    <Tag
                                        key={`${document.id}-expiry`}
                                        text={formatDateOnly(document.expiresAt)}
                                        tone={isExpired(document.expiresAt) ? "danger" : "neutral"}
                                    />,
                                    formatCurrency(document.cost)
                                ])}
                                emptyMessage="No documents for this vehicle."
                            />
                            {isAdmin ? (
                                <form className="detail-form" onSubmit={handleDocumentSubmit} noValidate>
                                    <DocumentForm form={documentForm} onChange={setDocumentForm}/>
                                    <div className="action-row">
                                        <button type="submit" className="primary-btn" disabled={isSaving}>
                                            {selectedDocument ? "Update document" : "Add document"}
                                        </button>
                                        <button
                                            type="button"
                                            className="secondary-btn"
                                            onClick={() => {
                                                setSelectedDocumentId(null);
                                                setDocumentForm(emptyDocumentForm);
                                            }}
                                        >
                                            New document
                                        </button>
                                        <button
                                            type="button"
                                            className="secondary-btn"
                                            onClick={handleDeleteDocument}
                                            disabled={!selectedDocument || isSaving}
                                        >
                                            Delete
                                        </button>
                                    </div>
                                </form>
                            ) : null}
                        </>
                    ) : (
                        <div className="detail-block">
                            <p>Select a vehicle to view documents.</p>
                        </div>
                    )}
                </section>
            </section>
        </div>
    );
}

function VehicleForm({
                         form,
                         depots,
                         onChange
                     }: {
    form: VehicleFormValues;
    depots: DepotResponse[];
    onChange: (nextForm: VehicleFormValues | ((current: VehicleFormValues) => VehicleFormValues)) => void;
}) {
    return (
        <>
            <div className="filter-grid">
                <label>
                    Depot
                    <select value={form.depotId}
                            onChange={(event) => onChange((current) => ({...current, depotId: event.target.value}))}
                            required>
                        <option value="">Select depot</option>
                        {depots.map((depot) => (
                            <option key={depot.id} value={depot.id}>
                                {depotLabel(depot)}
                            </option>
                        ))}
                    </select>
                </label>
                <label>
                    License plate
                    <input
                        value={form.licensePlate}
                        onChange={(event) => onChange((current) => ({...current, licensePlate: event.target.value}))}
                        maxLength={20}
                        required
                    />
                </label>
            </div>

            <div className="filter-grid">
                <label>
                    Brand
                    <input
                        value={form.brand}
                        onChange={(event) => onChange((current) => ({...current, brand: event.target.value}))}
                        maxLength={80}
                        required
                    />
                </label>
                <label>
                    Model
                    <input
                        value={form.model}
                        onChange={(event) => onChange((current) => ({...current, model: event.target.value}))}
                        maxLength={80}
                        required
                    />
                </label>
            </div>

            <div className="filter-grid">
                <label>
                    VIN
                    <input
                        value={form.vin}
                        onChange={(event) => onChange((current) => ({...current, vin: event.target.value}))}
                        maxLength={50}
                        required
                    />
                </label>
                <label>
                    Year
                    <input
                        type="number"
                        value={form.year}
                        onChange={(event) => onChange((current) => ({...current, year: event.target.value}))}
                        min={1900}
                        max={2100}
                        required
                    />
                </label>
            </div>

            <div className="filter-grid">
                <label>
                    Category
                    <select value={form.category}
                            onChange={(event) => onChange((current) => ({
                                ...current,
                                category: event.target.value as VehicleCategory
                            }))}
                            required>
                        {categoryOptions.map((category) => (
                            <option key={category} value={category}>{category}</option>
                        ))}
                    </select>
                </label>
                <label>
                    Fuel type
                    <select value={form.fuelType}
                            onChange={(event) => onChange((current) => ({
                                ...current,
                                fuelType: event.target.value as FuelType
                            }))}
                            required>
                        {fuelTypeOptions.map((fuelType) => (
                            <option key={fuelType} value={fuelType}>{fuelType}</option>
                        ))}
                    </select>
                </label>
            </div>

            <div className="filter-grid">
                <label>
                    Capacity weight
                    <input
                        type="number"
                        step="any"
                        value={form.capacityWeight}
                        onChange={(event) => onChange((current) => ({...current, capacityWeight: event.target.value}))}
                        required
                    />
                </label>
                <label>
                    Capacity volume
                    <input
                        type="number"
                        step="any"
                        value={form.capacityVolume}
                        onChange={(event) => onChange((current) => ({...current, capacityVolume: event.target.value}))}
                        required
                    />
                </label>
            </div>
        </>
    );
}

function MaintenanceForm({
                             form,
                             onChange
                         }: {
    form: MaintenanceFormValues;
    onChange: (nextForm: MaintenanceFormValues | ((current: MaintenanceFormValues) => MaintenanceFormValues)) => void;
}) {
    return (
        <>
            <div className="filter-grid">
                <label>
                    Service date
                    <input
                        type="date"
                        value={form.serviceDate}
                        onChange={(event) => onChange((current) => ({...current, serviceDate: event.target.value}))}
                        required
                    />
                </label>
                <label>
                    Mileage
                    <input
                        type="number"
                        min={0}
                        value={form.mileage}
                        onChange={(event) => onChange((current) => ({...current, mileage: event.target.value}))}
                        required
                    />
                </label>
            </div>
            <div className="filter-grid">
                <label>
                    Cost
                    <input
                        type="number"
                        min={0}
                        step="0.01"
                        value={form.cost}
                        onChange={(event) => onChange((current) => ({...current, cost: event.target.value}))}
                        required
                    />
                </label>
                <label>
                    Service provider
                    <input
                        value={form.serviceProvider}
                        onChange={(event) => onChange((current) => ({...current, serviceProvider: event.target.value}))}
                        maxLength={150}
                    />
                </label>
            </div>
            <label>
                Description
                <input
                    value={form.description}
                    onChange={(event) => onChange((current) => ({...current, description: event.target.value}))}
                    maxLength={500}
                />
            </label>
        </>
    );
}

function DocumentForm({
                          form,
                          onChange
                      }: {
    form: DocumentFormValues;
    onChange: (nextForm: DocumentFormValues | ((current: DocumentFormValues) => DocumentFormValues)) => void;
}) {
    return (
        <>
            <div className="filter-grid">
                <label>
                    Document type
                    <select
                        value={form.documentType}
                        onChange={(event) => onChange((current) => ({
                            ...current,
                            documentType: event.target.value as VehicleDocumentType
                        }))}
                        required
                    >
                        {documentTypeOptions.map((type) => (
                            <option key={type} value={type}>{type}</option>
                        ))}
                    </select>
                </label>
                <label>
                    Cost
                    <input
                        type="number"
                        min={0}
                        step="0.01"
                        value={form.cost}
                        onChange={(event) => onChange((current) => ({...current, cost: event.target.value}))}
                        required
                    />
                </label>
            </div>
            <div className="filter-grid">
                <label>
                    Issued at
                    <input
                        type="date"
                        value={form.issuedAt}
                        onChange={(event) => onChange((current) => ({...current, issuedAt: event.target.value}))}
                        required
                    />
                </label>
                <label>
                    Expires at
                    <input
                        type="date"
                        value={form.expiresAt}
                        onChange={(event) => onChange((current) => ({...current, expiresAt: event.target.value}))}
                        required
                    />
                </label>
            </div>
            <label>
                Description
                <input
                    value={form.description}
                    onChange={(event) => onChange((current) => ({...current, description: event.target.value}))}
                    maxLength={500}
                />
            </label>
        </>
    );
}

function SimpleTable({
                         columns,
                         rows,
                         emptyMessage
                     }: {
    columns: string[];
    rows: ReactNode[][];
    emptyMessage: string;
}) {
    return (
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
                        <tr key={`simple-row-${rowIndex}`}>
                            {row.map((cell, cellIndex) => (
                                <td key={`simple-cell-${rowIndex}-${cellIndex}`}>{cell}</td>
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
                <p>{loading ? "Loading..." : `${total} vehicles found`}</p>
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
                            <tr key={`vehicle-row-${rowIndex}`}>
                                {row.map((cell, cellIndex) => (
                                    <td key={`vehicle-cell-${rowIndex}-${cellIndex}`}>{cell}</td>
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

function toVehicleForm(vehicle: VehicleResponse): VehicleFormValues {
    return {
        depotId: vehicle.depotId ?? "",
        licensePlate: vehicle.licensePlate ?? "",
        brand: vehicle.brand ?? "",
        model: vehicle.model ?? "",
        vin: vehicle.vin ?? "",
        year: vehicle.year ? String(vehicle.year) : "",
        category: vehicle.category ?? "M1",
        fuelType: vehicle.fuelType ?? "DIESEL",
        capacityWeight: vehicle.capacityWeight ? String(vehicle.capacityWeight) : "",
        capacityVolume: vehicle.capacityVolume ? String(vehicle.capacityVolume) : ""
    };
}

function toVehiclePayload(form: VehicleFormValues): VehiclePayload {
    return {
        depotId: form.depotId,
        licensePlate: form.licensePlate.trim(),
        brand: form.brand.trim(),
        model: form.model.trim(),
        vin: form.vin.trim(),
        year: Number(form.year),
        category: form.category,
        fuelType: form.fuelType,
        capacityWeight: Number(form.capacityWeight),
        capacityVolume: Number(form.capacityVolume)
    };
}

function validateVehicleForm(form: VehicleFormValues) {
    if (!form.depotId) {
        return "Depot is required.";
    }

    if (!form.licensePlate.trim()) {
        return "License plate is required.";
    }

    if (!form.brand.trim()) {
        return "Brand is required.";
    }

    if (!form.model.trim()) {
        return "Model is required.";
    }

    if (!form.vin.trim()) {
        return "VIN is required.";
    }

    const year = Number(form.year);
    if (!Number.isInteger(year) || year < 1900 || year > 2100) {
        return "Year must be an integer between 1900 and 2100.";
    }

    const capacityWeight = Number(form.capacityWeight);
    if (!Number.isFinite(capacityWeight) || capacityWeight <= 0) {
        return "Capacity weight must be a positive number.";
    }

    const capacityVolume = Number(form.capacityVolume);
    if (!Number.isFinite(capacityVolume) || capacityVolume <= 0) {
        return "Capacity volume must be a positive number.";
    }

    return null;
}

function toMaintenanceForm(record: VehicleMaintenanceResponse): MaintenanceFormValues {
    return {
        mileage: record.mileage === null || record.mileage === undefined ? "" : String(record.mileage),
        serviceDate: record.serviceDate ?? "",
        cost: record.cost === null || record.cost === undefined ? "" : String(record.cost),
        description: record.description ?? "",
        serviceProvider: record.serviceProvider ?? ""
    };
}

function toMaintenancePayload(form: MaintenanceFormValues): VehicleMaintenancePayload {
    return {
        mileage: Number(form.mileage),
        serviceDate: form.serviceDate,
        cost: Number(form.cost),
        description: form.description.trim(),
        serviceProvider: form.serviceProvider.trim()
    };
}

function validateMaintenanceForm(form: MaintenanceFormValues) {
    if (!form.serviceDate) {
        return "Service date is required.";
    }

    const mileage = Number(form.mileage);
    if (!Number.isInteger(mileage) || mileage < 0) {
        return "Mileage must be a positive integer or zero.";
    }

    const cost = Number(form.cost);
    if (!Number.isFinite(cost) || cost < 0) {
        return "Maintenance cost must be a positive number or zero.";
    }

    return null;
}

function toDocumentForm(document: VehicleDocumentResponse): DocumentFormValues {
    return {
        documentType: document.documentType ?? "INSURANCE",
        issuedAt: document.issuedAt ?? "",
        expiresAt: document.expiresAt ?? "",
        cost: document.cost === null || document.cost === undefined ? "" : String(document.cost),
        description: document.description ?? ""
    };
}

function toDocumentPayload(form: DocumentFormValues): VehicleDocumentPayload {
    return {
        documentType: form.documentType,
        issuedAt: form.issuedAt,
        expiresAt: form.expiresAt,
        cost: Number(form.cost),
        description: form.description.trim()
    };
}

function validateDocumentForm(form: DocumentFormValues) {
    if (!form.documentType) {
        return "Document type is required.";
    }

    if (!form.issuedAt) {
        return "Issue date is required.";
    }

    if (!form.expiresAt) {
        return "Expiry date is required.";
    }

    if (new Date(form.expiresAt) < new Date(form.issuedAt)) {
        return "Expiry date cannot be before issue date.";
    }

    const cost = Number(form.cost);
    if (!Number.isFinite(cost) || cost < 0) {
        return "Document cost must be a positive number or zero.";
    }

    return null;
}

function depotLabel(depot: DepotResponse | null | undefined) {
    return formatDepotLabel(depot);
}

function courierLabel(courier: CourierResponse | null | undefined, fallbackId: string) {
    return formatCourierLabel(courier, fallbackId);
}

function courierProfileLink(courier: CourierResponse | null | undefined, fallbackId: string) {
    const fullName = courier ? `${courier.user.firstName} ${courier.user.lastName}`.trim() : "";
    const search = courier?.user.email?.trim() || fullName || fallbackId.trim();

    if (!search) {
        return "/couriers";
    }

    const query = new URLSearchParams({search});
    return `/couriers?${query.toString()}`;
}

function statusTone(status: VehicleStatus) {
    if (status === "AVAILABLE") {
        return "success";
    }

    if (status === "OUT_OF_SERVICE") {
        return "danger";
    }

    return "warning";
}

function formatNumber(value: number | null | undefined) {
    return typeof value === "number" ? new Intl.NumberFormat("ro-RO", {maximumFractionDigits: 2}).format(value) : "-";
}

function formatCurrency(value: number | null | undefined) {
    return typeof value === "number"
        ? new Intl.NumberFormat("ro-RO", {style: "currency", currency: "RON"}).format(value)
        : "-";
}

function formatDateOnly(value: string | null | undefined) {
    if (!value) {
        return "-";
    }

    return new Intl.DateTimeFormat("ro-RO", {dateStyle: "medium"}).format(new Date(value));
}

function formatDate(value: string | null | undefined) {
    if (!value) {
        return "-";
    }

    return new Intl.DateTimeFormat("ro-RO", {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(new Date(value));
}

function isExpired(value: string) {
    const expiresAt = new Date(value);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return expiresAt < today;
}

