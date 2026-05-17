import {useEffect, useMemo, useState} from "react";
import {ApiError} from "@/api/httpClient";
import {PageHeader} from "@/components/PageHeader";
import {PageFeedback} from "@/components/PageFeedback";
import {TableCard} from "@/components/TableCard";
import {Tag} from "@/components/Tag";
import {type LocalityResponse, resolveCoordinates, searchLocalities} from "@/features/geography/geographyApi";
import {
    createParcel,
    type DepotOption,
    getDepotOptions,
    getParcel,
    getParcelPinByAwb,
    type ParcelBackendStatus,
    type ParcelDetails,
    type ParcelSummary,
    searchParcels,
    updateParcel,
    type UpdateParcelRequest,
    updateParcelStatus,
    verifyDeliveryCode
} from "@/features/parcels/parcelApi";

const statusOptions: ParcelBackendStatus[] = [
    "CREATED",
    "SENDER_REGIONAL_COURIER_ASSIGNED",
    "PICKED_UP_BY_SENDER_REGIONAL_COURIER",
    "IN_SENDER_REGIONAL_DEPOSIT",
    "TRANSIT_COURIER_ASSIGNED",
    "IN_TRANSIT",
    "IN_RECEIVER_REGIONAL_DEPOSIT",
    "RECEIVER_REGIONAL_COURIER_ASSIGNED",
    "IN_DELIVERY",
    "DELIVERED",
    "WAITING_IN_DEPOT"
];

const validationFieldLabels: Record<string, string> = {
    depotCode: "Depot code",
    weight: "Weight",
    volume: "Volume",
    paymentAmount: "Payment amount",
    declaredValue: "Declared value",
    observations: "Observations",
    "senderContact.name": "Sender name",
    "senderContact.email": "Sender email",
    "senderContact.phone": "Sender phone",
    "senderContact.address.county": "Sender county",
    "senderContact.address.city": "Sender city",
    "senderContact.address.street": "Sender street",
    "senderContact.address.number": "Sender street number",
    "receiverContact.name": "Receiver name",
    "receiverContact.email": "Receiver email",
    "receiverContact.phone": "Receiver phone",
    "receiverContact.address.county": "Receiver county",
    "receiverContact.address.city": "Receiver city",
    "receiverContact.address.street": "Receiver street",
    "receiverContact.address.number": "Receiver street number"
};

interface ParcelEditDraft {
    depotCode: string;
    weight: string;
    volume: string;
    paymentRequired: boolean;
    paymentAmount: string;
    declaredValue: string;
    observations: string;
    senderName: string;
    senderEmail: string;
    senderPhone: string;
    senderCounty: string;
    senderCity: string;
    senderStreet: string;
    senderNumber: string;
    receiverName: string;
    receiverEmail: string;
    receiverPhone: string;
    receiverCounty: string;
    receiverCity: string;
    receiverStreet: string;
    receiverNumber: string;
}

function getStatusTone(status: ParcelBackendStatus): "neutral" | "success" | "warning" {
    if (status === "DELIVERED") {
        return "success";
    }
    if (status === "WAITING_IN_DEPOT") {
        return "warning";
    }
    return "neutral";
}

function toErrorMessage(error: unknown): string {
    if (error instanceof ApiError) {
        const fieldErrors = extractFieldErrors(error.details);
        if (fieldErrors.length > 0) {
            return `${error.message}. ${fieldErrors.join("; ")}`;
        }
        return error.message;
    }
    if (error instanceof Error) {
        return error.message;
    }
    return "Operation failed.";
}

function extractFieldErrors(details?: Record<string, unknown>): string[] {
    const fields = details?.fields;
    if (!fields || typeof fields !== "object" || Array.isArray(fields)) {
        return [];
    }

    return Object.entries(fields)
        .filter((entry): entry is [string, string] => typeof entry[1] === "string")
        .map(([field, message]) => `${toValidationFieldLabel(field)}: ${message}`);
}

function toValidationFieldLabel(field: string): string {
    const label = validationFieldLabels[field];
    if (label) {
        return label;
    }
    return field.replaceAll(".", " ");
}

function contactAddressLabel(parcel: ParcelDetails, contactType: "senderContact" | "receiverContact"): string {
    const contact = parcel[contactType];
    const address = contact.address;
    return `${address.street} ${address.number}`;
}

function depotDisplayLabel(depotCode: string | null | undefined, depots: DepotOption[]): string {
    const code = (depotCode ?? "").trim();
    if (!code) {
        return "-";
    }
    const depot = depots.find((entry) => entry.code === code);
    if (!depot) {
        return code;
    }
    return `${depot.code} (${depot.name})`;
}

function toInputValue(value: number | string | null | undefined): string {
    if (value === null || value === undefined) {
        return "";
    }
    return String(value);
}

function buildEditDraft(parcel: ParcelDetails): ParcelEditDraft {
    return {
        depotCode: parcel.depotCode,
        weight: toInputValue(parcel.weight),
        volume: toInputValue(parcel.volume),
        paymentRequired: parcel.paymentRequired,
        paymentAmount: toInputValue(parcel.paymentAmount),
        declaredValue: toInputValue(parcel.declaredValue),
        observations: parcel.observations ?? "",
        senderName: parcel.senderContact.name,
        senderEmail: parcel.senderContact.email ?? "",
        senderPhone: parcel.senderContact.phone,
        senderCounty: parcel.senderContact.address.county,
        senderCity: parcel.senderContact.address.city,
        senderStreet: parcel.senderContact.address.street,
        senderNumber: parcel.senderContact.address.number,
        receiverName: parcel.receiverContact.name,
        receiverEmail: parcel.receiverContact.email ?? "",
        receiverPhone: parcel.receiverContact.phone,
        receiverCounty: parcel.receiverContact.address.county,
        receiverCity: parcel.receiverContact.address.city,
        receiverStreet: parcel.receiverContact.address.street,
        receiverNumber: parcel.receiverContact.address.number
    };
}

function mapDetailsToSummary(parcel: ParcelDetails, currentSummary?: ParcelSummary): ParcelSummary {
    return {
        id: parcel.id,
        awb: parcel.awb,
        depotCode: parcel.depotCode,
        receiverName: parcel.receiverContact.name,
        receiverCity: parcel.receiverContact.address.city,
        receiverCounty: parcel.receiverContact.address.county,
        weight: parcel.weight,
        volume: parcel.volume,
        status: parcel.status,
        createdAt: currentSummary?.createdAt ?? parcel.createdAt
    };
}

function parseRequiredNumber(value: string): number | null {
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) {
        return null;
    }
    return parsed;
}

function parseOptionalNumber(value: string): number | undefined {
    if (value.trim().length === 0) {
        return undefined;
    }
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) {
        return undefined;
    }
    return parsed;
}

function buildCreateDraft(defaultDepotCode?: string): ParcelEditDraft {
    return {
        depotCode: defaultDepotCode ?? "",
        weight: "1",
        volume: "0.1",
        paymentRequired: false,
        paymentAmount: "",
        declaredValue: "",
        observations: "",
        senderName: "",
        senderEmail: "",
        senderPhone: "",
        senderCounty: "",
        senderCity: "",
        senderStreet: "",
        senderNumber: "",
        receiverName: "",
        receiverEmail: "",
        receiverPhone: "",
        receiverCounty: "",
        receiverCity: "",
        receiverStreet: "",
        receiverNumber: ""
    };
}

export function ParcelsPage() {
    const [parcels, setParcels] = useState<ParcelSummary[]>([]);
    const [totalParcels, setTotalParcels] = useState(0);
    const [currentPage, setCurrentPage] = useState(0);
    const [depots, setDepots] = useState<DepotOption[]>([]);
    const [selectedParcelId, setSelectedParcelId] = useState("");
    const [selectedParcel, setSelectedParcel] = useState<ParcelDetails | null>(null);
    const [nextStatus, setNextStatus] = useState<ParcelBackendStatus>("CREATED");

    const [isEditing, setIsEditing] = useState(false);
    const [isCreating, setIsCreating] = useState(false);
    const [editDraft, setEditDraft] = useState<ParcelEditDraft | null>(null);

    const [search, setSearch] = useState("");
    const [depotFilter, setDepotFilter] = useState("ALL");
    const [statusFilter, setStatusFilter] = useState<ParcelBackendStatus | "ALL">("ALL");
    const [counties, setCounties] = useState<LocalityResponse[]>([]);
    const [countiesError, setCountiesError] = useState<string | null>(null);
    const [depotsError, setDepotsError] = useState<string | null>(null);

    const [isListLoading, setIsListLoading] = useState(false);
    const [isDetailsLoading, setIsDetailsLoading] = useState(false);
    const [isStatusUpdating, setIsStatusUpdating] = useState(false);
    const [isUpdatingParcel, setIsUpdatingParcel] = useState(false);

    const [listError, setListError] = useState<string | null>(null);
    const [detailsError, setDetailsError] = useState<string | null>(null);
    const [actionMessage, setActionMessage] = useState<string | null>(null);
    const [actionError, setActionError] = useState<string | null>(null);

    const [pinValue, setPinValue] = useState<string | null>(null);
    const [verifyPinInput, setVerifyPinInput] = useState("");
    const [verifyResult, setVerifyResult] = useState<boolean | null>(null);
    const pageSize = 10;
    const totalPages = Math.max(1, Math.ceil(totalParcels / pageSize));

    const filteredParcels = useMemo(() => {
        const term = search.trim().toLowerCase();
        if (!term) {
            return parcels;
        }
        return parcels.filter(
            (parcel) =>
                parcel.awb.toLowerCase().includes(term) ||
                parcel.receiverName.toLowerCase().includes(term)
        );
    }, [parcels, search]);

    const sortedCounties = useMemo(
        () =>
            [...counties].sort((first, second) =>
                `${first.name} ${first.region.code}`.localeCompare(`${second.name} ${second.region.code}`)
            ),
        [counties]
    );

    useEffect(() => {
        let isCancelled = false;

        async function loadDepots() {
            setDepotsError(null);
            try {
                const rows = await getDepotOptions();
                if (!isCancelled) {
                    setDepots(rows);
                }
            } catch (error) {
                if (!isCancelled) {
                    setDepotsError(toErrorMessage(error));
                    setDepots([]);
                }
            }
        }

        void loadDepots();

        return () => {
            isCancelled = true;
        };
    }, []);

    useEffect(() => {
        let isCancelled = false;

        async function loadCounties() {
            setCountiesError(null);
            try {
                const response = await searchLocalities({page: 0, size: 2000});
                if (!isCancelled) {
                    setCounties(response.content);
                }
            } catch (error) {
                if (!isCancelled) {
                    setCountiesError(toErrorMessage(error));
                    setCounties([]);
                }
            }
        }

        void loadCounties();

        return () => {
            isCancelled = true;
        };
    }, []);

    useEffect(() => {
        let isCancelled = false;

        async function loadParcels() {
            setIsListLoading(true);
            setListError(null);

            try {
                const response = await searchParcels({
                    depotCode: depotFilter === "ALL" ? undefined : depotFilter,
                    status: statusFilter === "ALL" ? undefined : statusFilter,
                    page: currentPage,
                    size: pageSize
                });

                if (isCancelled) {
                    return;
                }

                setParcels(response.content);
                setTotalParcels(response.totalElements);
                setSelectedParcelId((previous) =>
                    response.content.some((parcel) => parcel.id === previous)
                        ? previous
                        : response.content[0]?.id ?? ""
                );
            } catch (error) {
                if (!isCancelled) {
                    setListError(toErrorMessage(error));
                    setParcels([]);
                    setTotalParcels(0);
                    setSelectedParcelId("");
                }
            } finally {
                if (!isCancelled) {
                    setIsListLoading(false);
                }
            }
        }

        void loadParcels();

        return () => {
            isCancelled = true;
        };
    }, [depotFilter, statusFilter, currentPage]);

    useEffect(() => {
        setCurrentPage(0);
    }, [depotFilter, statusFilter]);

    useEffect(() => {
        let isCancelled = false;

        async function loadSelectedParcel() {
            if (!selectedParcelId) {
                setSelectedParcel(null);
                setDetailsError(null);
                setPinValue(null);
                setVerifyResult(null);
                setVerifyPinInput("");
                setIsEditing(false);
                setEditDraft(null);
                return;
            }

            setIsDetailsLoading(true);
            setDetailsError(null);
            setActionError(null);
            setActionMessage(null);
            setPinValue(null);
            setVerifyResult(null);
            setVerifyPinInput("");
            setIsEditing(false);
            setIsCreating(false);

            try {
                const parcel = await getParcel(selectedParcelId);
                if (!isCancelled) {
                    setSelectedParcel(parcel);
                    setNextStatus(parcel.status);
                    setEditDraft(buildEditDraft(parcel));
                }
            } catch (error) {
                if (!isCancelled) {
                    setDetailsError(toErrorMessage(error));
                    setSelectedParcel(null);
                    setEditDraft(null);
                }
            } finally {
                if (!isCancelled) {
                    setIsDetailsLoading(false);
                }
            }
        }

        void loadSelectedParcel();

        return () => {
            isCancelled = true;
        };
    }, [selectedParcelId]);

    function applyUpdatedParcel(parcel: ParcelDetails) {
        setSelectedParcel(parcel);
        setNextStatus(parcel.status);
        setEditDraft(buildEditDraft(parcel));

        setParcels((current) =>
            current.map((entry) =>
                entry.id === parcel.id ? mapDetailsToSummary(parcel, entry) : entry
            )
        );
    }

    function applyCreatedParcel(parcel: ParcelDetails) {
        setParcels((current) => [mapDetailsToSummary(parcel), ...current]);
        setTotalParcels((current) => current + 1);
        setSelectedParcelId(parcel.id);
        setSelectedParcel(parcel);
        setNextStatus(parcel.status);
        setEditDraft(buildEditDraft(parcel));
    }

    async function handleChangeStatus() {
        if (!selectedParcel) {
            return;
        }

        setIsStatusUpdating(true);
        setActionError(null);
        setActionMessage(null);

        try {
            await updateParcelStatus(selectedParcel.id, nextStatus);
            const refreshed = await getParcel(selectedParcel.id);
            applyUpdatedParcel(refreshed);
            setActionMessage(`Status changed to ${nextStatus}.`);
        } catch (error) {
            setActionError(toErrorMessage(error));
        } finally {
            setIsStatusUpdating(false);
        }
    }

    async function handleSaveEdit() {
        if (!editDraft) {
            return;
        }

        const weight = parseRequiredNumber(editDraft.weight);
        const volume = parseRequiredNumber(editDraft.volume);

        if (!isCreating && !editDraft.depotCode.trim()) {
            setActionError("Depot code is required.");
            return;
        }
        if (editDraft.depotCode.trim() && !depots.some((depot) => depot.code === editDraft.depotCode.trim())) {
            setActionError("Selected depot code is not valid.");
            return;
        }
        if (!editDraft.senderName.trim() || !editDraft.receiverName.trim()) {
            setActionError("Sender and receiver names are required.");
            return;
        }
        if (!editDraft.senderPhone.trim() || !editDraft.receiverPhone.trim()) {
            setActionError("Sender and receiver phone are required.");
            return;
        }
        if (
            !editDraft.senderCounty.trim() ||
            !editDraft.senderCity.trim() ||
            !editDraft.receiverCounty.trim() ||
            !editDraft.receiverCity.trim()
        ) {
            setActionError("Sender and receiver county/city are required.");
            return;
        }
        if (!editDraft.senderStreet.trim() || !editDraft.receiverStreet.trim()) {
            setActionError("Sender and receiver street are required.");
            return;
        }
        if (!editDraft.senderNumber.trim() || !editDraft.receiverNumber.trim()) {
            setActionError("Sender and receiver street number are required.");
            return;
        }
        if (weight === null || volume === null) {
            setActionError("Weight and volume must be valid numbers.");
            return;
        }

        try {
            await Promise.all([
                resolveCoordinates({
                    street: `${editDraft.senderStreet.trim()} ${editDraft.senderNumber.trim()}`,
                    city: editDraft.senderCity.trim(),
                    county: editDraft.senderCounty.trim()
                }),
                resolveCoordinates({
                    street: `${editDraft.receiverStreet.trim()} ${editDraft.receiverNumber.trim()}`,
                    city: editDraft.receiverCity.trim(),
                    county: editDraft.receiverCounty.trim()
                })
            ]);
        } catch {
            setActionError("Address validation failed for selected city/region. Please verify street and city.");
            return;
        }

        const payload: UpdateParcelRequest = {
            depotCode: editDraft.depotCode.trim(),
            senderContact: {
                name: editDraft.senderName.trim(),
                email: editDraft.senderEmail.trim() || null,
                phone: editDraft.senderPhone.trim(),
                address: {
                    county: editDraft.senderCounty.trim(),
                    city: editDraft.senderCity.trim(),
                    street: editDraft.senderStreet.trim(),
                    number: editDraft.senderNumber.trim()
                }
            },
            receiverContact: {
                name: editDraft.receiverName.trim(),
                email: editDraft.receiverEmail.trim() || null,
                phone: editDraft.receiverPhone.trim(),
                address: {
                    county: editDraft.receiverCounty.trim(),
                    city: editDraft.receiverCity.trim(),
                    street: editDraft.receiverStreet.trim(),
                    number: editDraft.receiverNumber.trim()
                }
            },
            weight,
            volume,
            paymentRequired: editDraft.paymentRequired,
            paymentAmount: parseOptionalNumber(editDraft.paymentAmount),
            declaredValue: parseOptionalNumber(editDraft.declaredValue),
            observations: editDraft.observations.trim() || undefined
        };

        setIsUpdatingParcel(true);
        setActionError(null);
        setActionMessage(null);

        try {
            if (isCreating) {
                const createPayload = {
                    senderContact: payload.senderContact!,
                    receiverContact: payload.receiverContact!,
                    weight,
                    volume,
                    paymentRequired: editDraft.paymentRequired,
                    paymentAmount: parseOptionalNumber(editDraft.paymentAmount),
                    declaredValue: parseOptionalNumber(editDraft.declaredValue),
                    observations: editDraft.observations.trim() || undefined
                };
                const created = await createParcel(createPayload);
                applyCreatedParcel(created);
            } else if (selectedParcel) {
                const updated = await updateParcel(selectedParcel.id, payload);
                applyUpdatedParcel(updated);
            }
            setIsEditing(false);
            setIsCreating(false);
            setActionMessage(isCreating ? "Parcel created." : "Parcel details updated.");
        } catch (error) {
            setActionError(toErrorMessage(error));
        } finally {
            setIsUpdatingParcel(false);
        }
    }

    async function handleLoadPin() {
        if (!selectedParcel) {
            return;
        }

        setActionError(null);
        setActionMessage(null);
        setPinValue(null);

        try {
            const result = await getParcelPinByAwb(selectedParcel.awb);
            setPinValue(result.pin);
            setActionMessage("Delivery PIN loaded from backend.");
        } catch (error) {
            setActionError(toErrorMessage(error));
        }
    }

    async function handleVerifyPin() {
        if (!selectedParcel || !verifyPinInput.trim()) {
            return;
        }

        setActionError(null);
        setActionMessage(null);
        setVerifyResult(null);

        try {
            const result = await verifyDeliveryCode(selectedParcel.awb, verifyPinInput.trim());
            setVerifyResult(result.valid);
        } catch (error) {
            setActionError(toErrorMessage(error));
        }
    }

    return (
        <div className="page-stack">
            <PageHeader
                title="Parcels"
                subtitle=""
                actions={
                    <div className="filter-row">
                        {depotsError ? <p className="form-error">{depotsError}</p> : null}
                        <input
                            className="text-input"
                            value={search}
                            onChange={(event) => setSearch(event.target.value)}
                            placeholder="Search by AWB or receiver"
                        />
                        <select
                            className="role-select"
                            value={depotFilter}
                            onChange={(event) => setDepotFilter(event.target.value)}
                        >
                            <option value="ALL">All depots</option>
                            {depots.map((depot) => (
                                <option key={depot.id} value={depot.code}>
                                    {depotDisplayLabel(depot.code, depots)}
                                </option>
                            ))}
                        </select>
                        <select
                            className="role-select"
                            value={statusFilter}
                            onChange={(event) =>
                                setStatusFilter(event.target.value as ParcelBackendStatus | "ALL")
                            }
                        >
                            <option value="ALL">All statuses</option>
                            {statusOptions.map((status) => (
                                <option key={status} value={status}>
                                    {status}
                                </option>
                            ))}
                        </select>
                    </div>
                }
            />
            <PageFeedback error={actionError} notice={actionMessage}/>

            <section className="two-column-grid">
                <TableCard
                    title="Parcel List"
                    caption={
                        isListLoading
                            ? "Loading parcels from backend..."
                            : `Page ${currentPage + 1} of ${totalPages} (${totalParcels} total)`
                    }
                    headerAction={
                        <button
                            className="primary-btn"
                            type="button"
                            onClick={() => {
                                setIsCreating(true);
                                setIsEditing(true);
                                setSelectedParcel(null);
                                setSelectedParcelId("");
                                setActionError(null);
                                setActionMessage(null);
                                setEditDraft(buildCreateDraft(depots[0]?.code));
                            }}
                            disabled={isUpdatingParcel || depots.length === 0}
                        >
                            Create Parcel
                        </button>
                    }
                    columns={["AWB", "Depot", "Status", "Receiver"]}
                    loading={isListLoading}
                    rows={filteredParcels.map((parcel) => {
                        return [
                            <button
                                key={`${parcel.id}-select`}
                                type="button"
                                className={parcel.id === selectedParcelId ? "text-link active-row" : "text-link"}
                                onClick={() => setSelectedParcelId(parcel.id)}
                            >
                                {parcel.awb}
                            </button>,
                            depotDisplayLabel(parcel.depotCode, depots),
                            <Tag key={`${parcel.id}-status`} text={parcel.status} tone={getStatusTone(parcel.status)}/>,
                            parcel.receiverName
                        ];
                    })}
                    footer={
                        <div className="pagination-row">
                            <button
                                className="secondary-btn"
                                type="button"
                                onClick={() => setCurrentPage((current) => Math.max(0, current - 1))}
                                disabled={isListLoading || currentPage === 0}
                            >
                                Previous
                            </button>
                            <span>
                                Page {currentPage + 1} of {totalPages}
                            </span>
                            <button
                                className="secondary-btn"
                                type="button"
                                onClick={() =>
                                    setCurrentPage((current) => (current + 1 < totalPages ? current + 1 : current))
                                }
                                disabled={isListLoading || currentPage + 1 >= totalPages}
                            >
                                Next
                            </button>
                        </div>
                    }
                />

                <section className="table-card">
                    <div className="table-card-head">
                        <div className="table-card-head-row">
                            <div>
                                <h3>Parcel Details</h3>
                            </div>
                            {selectedParcel || isCreating ? (
                                <button
                                    className="secondary-btn"
                                    type="button"
                                    onClick={() => {
                                        if (isEditing) {
                                            setIsEditing(false);
                                            setIsCreating(false);
                                            if (selectedParcel) {
                                                setEditDraft(buildEditDraft(selectedParcel));
                                            } else {
                                                setEditDraft(null);
                                            }
                                        } else {
                                            setIsEditing(true);
                                            if (selectedParcel) {
                                                setEditDraft(buildEditDraft(selectedParcel));
                                            }
                                        }
                                    }}
                                    disabled={isDetailsLoading || isUpdatingParcel}
                                >
                                    {isEditing ? "Cancel Edit" : "Edit"}
                                </button>
                            ) : null}
                        </div>
                    </div>

                    {listError ? (
                        <div className="detail-block">
                            <p className="form-error">{listError}</p>
                        </div>
                    ) : null}

                    {isDetailsLoading && !selectedParcel ? (
                        <div className="detail-block">
                            <p>Loading parcel details...</p>
                        </div>
                    ) : null}

                    {detailsError ? (
                        <div className="detail-block">
                            <p className="form-error">{detailsError}</p>
                        </div>
                    ) : null}

                    {!detailsError && selectedParcel && !isEditing && !isCreating ? (
                        <div className="detail-block">
                            {isDetailsLoading ? <p className="topbar-label">Refreshing parcel details...</p> : null}
                            <p>
                                <strong>AWB:</strong> {selectedParcel.awb}
                            </p>
                            <p>
                                <strong>Depot:</strong> {depotDisplayLabel(selectedParcel.depotCode, depots)}
                            </p>
                            <p>
                                <strong>Status:</strong> {selectedParcel.status}
                            </p>
                            <p>
                                <strong>Receiver:</strong> {selectedParcel.receiverContact.name} ({selectedParcel.receiverContact.phone})
                            </p>
                            <p>
                                <strong>Receiver
                                    address:</strong> {contactAddressLabel(selectedParcel, "receiverContact")}
                            </p>
                            <p>
                                <strong>Sender:</strong> {selectedParcel.senderContact.name} ({selectedParcel.senderContact.phone})
                            </p>
                            <p>
                                <strong>Sender address:</strong> {contactAddressLabel(selectedParcel, "senderContact")}
                            </p>
                            <p>
                                <strong>Weight / Volume:</strong> {selectedParcel.weight} kg / {selectedParcel.volume}
                            </p>
                            <p>
                                <strong>Payment required:</strong> {selectedParcel.paymentRequired ? "Yes" : "No"}
                            </p>
                            <p>
                                <strong>Payment /
                                    Declared:</strong> {selectedParcel.paymentAmount ?? "-"} / {selectedParcel.declaredValue ?? "-"}
                            </p>
                            <p>
                                <strong>Updated:</strong> {selectedParcel.updatedAt}
                            </p>

                            <div className="filter-row">
                                <select
                                    className="role-select"
                                    value={nextStatus}
                                    onChange={(event) => setNextStatus(event.target.value as ParcelBackendStatus)}
                                >
                                    {statusOptions.map((status) => (
                                        <option key={status} value={status}>
                                            {status}
                                        </option>
                                    ))}
                                </select>
                                <button
                                    className="secondary-btn"
                                    type="button"
                                    onClick={() => void handleChangeStatus()}
                                    disabled={isStatusUpdating || nextStatus === selectedParcel.status}
                                >
                                    {isStatusUpdating ? "Changing..." : "Change Status"}
                                </button>
                            </div>

                            <div className="action-row">
                                <button className="secondary-btn" type="button" onClick={() => void handleLoadPin()}>
                                    Load Delivery PIN
                                </button>
                            </div>

                            <div className="filter-row">
                                <input
                                    className="text-input"
                                    value={verifyPinInput}
                                    onChange={(event) => setVerifyPinInput(event.target.value)}
                                    placeholder="Verify delivery code PIN"
                                />
                                <button className="secondary-btn" type="button" onClick={() => void handleVerifyPin()}>
                                    Verify Code
                                </button>
                            </div>

                            {pinValue ? (
                                <p>
                                    <strong>Current PIN:</strong> {pinValue}
                                </p>
                            ) : null}
                            {verifyResult !== null ? (
                                <p className={verifyResult ? "form-success" : "form-error"}>
                                    {verifyResult ? "Code is valid." : "Code is invalid."}
                                </p>
                            ) : null}
                        </div>
                    ) : null}

                    {!detailsError && (selectedParcel || isCreating) && isEditing && editDraft ? (
                        <div className="detail-block">
                            {countiesError ? <p className="form-error">{countiesError}</p> : null}
                            {!countiesError && sortedCounties.length === 0 ? (
                                <p className="form-error">No counties were loaded from geography service.</p>
                            ) : null}

                            <div className="detail-form-grid">
                                <label>
                                    Depot
                                    <select
                                        className="role-select"
                                        value={editDraft.depotCode}
                                        onChange={(event) =>
                                            setEditDraft((current) => current ? {
                                                ...current,
                                                depotCode: event.target.value
                                            } : current)
                                        }
                                    >
                                        <option value="">Select depot</option>
                                        {depots.map((depot) => (
                                            <option key={depot.id} value={depot.code}>
                                                {depotDisplayLabel(depot.code, depots)}
                                            </option>
                                        ))}
                                    </select>
                                </label>
                                <label>
                                    Weight (kg)
                                    <input
                                        className="text-input"
                                        type="number"
                                        step="0.01"
                                        value={editDraft.weight}
                                        onChange={(event) =>
                                            setEditDraft((current) => current ? {
                                                ...current,
                                                weight: event.target.value
                                            } : current)
                                        }
                                    />
                                </label>
                                <label>
                                    Volume
                                    <input
                                        className="text-input"
                                        type="number"
                                        step="0.01"
                                        value={editDraft.volume}
                                        onChange={(event) =>
                                            setEditDraft((current) => current ? {
                                                ...current,
                                                volume: event.target.value
                                            } : current)
                                        }
                                    />
                                </label>
                                <label>
                                    Payment amount
                                    <input
                                        className="text-input"
                                        type="number"
                                        step="0.01"
                                        value={editDraft.paymentAmount}
                                        onChange={(event) =>
                                            setEditDraft((current) => current ? {
                                                ...current,
                                                paymentAmount: event.target.value
                                            } : current)
                                        }
                                    />
                                </label>
                                <label>
                                    Declared value
                                    <input
                                        className="text-input"
                                        type="number"
                                        step="0.01"
                                        value={editDraft.declaredValue}
                                        onChange={(event) =>
                                            setEditDraft((current) => current ? {
                                                ...current,
                                                declaredValue: event.target.value
                                            } : current)
                                        }
                                    />
                                </label>
                                <label>
                                    Observations
                                    <input
                                        className="text-input"
                                        value={editDraft.observations}
                                        onChange={(event) =>
                                            setEditDraft((current) => current ? {
                                                ...current,
                                                observations: event.target.value
                                            } : current)
                                        }
                                    />
                                </label>
                            </div>

                            <p><strong>Sender</strong></p>
                            <div className="detail-form-grid">
                                <label>
                                    Name
                                    <input className="text-input" value={editDraft.senderName}
                                           onChange={(event) => setEditDraft((current) => current ? {
                                               ...current,
                                               senderName: event.target.value
                                           } : current)}/>
                                </label>
                                <label>
                                    Email
                                    <input className="text-input" value={editDraft.senderEmail}
                                           onChange={(event) => setEditDraft((current) => current ? {
                                               ...current,
                                               senderEmail: event.target.value
                                           } : current)}/>
                                </label>
                                <label>
                                    Phone
                                    <input className="text-input" value={editDraft.senderPhone}
                                           onChange={(event) => setEditDraft((current) => current ? {
                                               ...current,
                                               senderPhone: event.target.value
                                           } : current)}/>
                                </label>
                                <label>
                                    County
                                    <select
                                        className="role-select"
                                        value={editDraft.senderCounty}
                                        onChange={(event) =>
                                            setEditDraft((current) =>
                                                current ? {...current, senderCounty: event.target.value} : current
                                            )
                                        }
                                    >
                                        <option value="">Select county</option>
                                        {sortedCounties.map((county) => (
                                            <option key={county.id} value={county.name}>
                                                {county.name} ({county.region.code})
                                            </option>
                                        ))}
                                    </select>
                                </label>
                                <label>
                                    City
                                    <input
                                        className="text-input"
                                        value={editDraft.senderCity}
                                        onChange={(event) =>
                                            setEditDraft((current) =>
                                                current ? {...current, senderCity: event.target.value} : current
                                            )
                                        }
                                    />
                                </label>
                                <label>
                                    Street
                                    <input className="text-input" value={editDraft.senderStreet}
                                           onChange={(event) => setEditDraft((current) => current ? {
                                               ...current,
                                               senderStreet: event.target.value
                                           } : current)}/>
                                </label>
                                <label>
                                    Number
                                    <input className="text-input" value={editDraft.senderNumber}
                                           onChange={(event) => setEditDraft((current) => current ? {
                                               ...current,
                                               senderNumber: event.target.value
                                           } : current)}/>
                                </label>
                            </div>

                            <p><strong>Receiver</strong></p>
                            <div className="detail-form-grid">
                                <label>
                                    Name
                                    <input className="text-input" value={editDraft.receiverName}
                                           onChange={(event) => setEditDraft((current) => current ? {
                                               ...current,
                                               receiverName: event.target.value
                                           } : current)}/>
                                </label>
                                <label>
                                    Email
                                    <input className="text-input" value={editDraft.receiverEmail}
                                           onChange={(event) => setEditDraft((current) => current ? {
                                               ...current,
                                               receiverEmail: event.target.value
                                           } : current)}/>
                                </label>
                                <label>
                                    Phone
                                    <input className="text-input" value={editDraft.receiverPhone}
                                           onChange={(event) => setEditDraft((current) => current ? {
                                               ...current,
                                               receiverPhone: event.target.value
                                           } : current)}/>
                                </label>
                                <label>
                                    County
                                    <select
                                        className="role-select"
                                        value={editDraft.receiverCounty}
                                        onChange={(event) =>
                                            setEditDraft((current) =>
                                                current ? {...current, receiverCounty: event.target.value} : current
                                            )
                                        }
                                    >
                                        <option value="">Select county</option>
                                        {sortedCounties.map((county) => (
                                            <option key={county.id} value={county.name}>
                                                {county.name} ({county.region.code})
                                            </option>
                                        ))}
                                    </select>
                                </label>
                                <label>
                                    City
                                    <input
                                        className="text-input"
                                        value={editDraft.receiverCity}
                                        onChange={(event) =>
                                            setEditDraft((current) =>
                                                current ? {...current, receiverCity: event.target.value} : current
                                            )
                                        }
                                    />
                                </label>
                                <label>
                                    Street
                                    <input className="text-input" value={editDraft.receiverStreet}
                                           onChange={(event) => setEditDraft((current) => current ? {
                                               ...current,
                                               receiverStreet: event.target.value
                                           } : current)}/>
                                </label>
                                <label>
                                    Number
                                    <input className="text-input" value={editDraft.receiverNumber}
                                           onChange={(event) => setEditDraft((current) => current ? {
                                               ...current,
                                               receiverNumber: event.target.value
                                           } : current)}/>
                                </label>
                            </div>

                            <label className="detail-checkbox">
                                <input
                                    type="checkbox"
                                    checked={editDraft.paymentRequired}
                                    onChange={(event) =>
                                        setEditDraft((current) => current ? {
                                            ...current,
                                            paymentRequired: event.target.checked
                                        } : current)
                                    }
                                />
                                Payment required
                            </label>

                            <div className="action-row">
                                <button
                                    className="primary-btn"
                                    type="button"
                                    onClick={() => void handleSaveEdit()}
                                    disabled={isUpdatingParcel}
                                >
                                    {isUpdatingParcel ? (isCreating ? "Creating..." : "Updating...") : (isCreating ? "Create Parcel" : "Update Parcel")}
                                </button>
                                <button
                                    className="secondary-btn"
                                    type="button"
                                    onClick={() => {
                                        setIsEditing(false);
                                        if (selectedParcel) {
                                            setEditDraft(buildEditDraft(selectedParcel));
                                        }
                                    }}
                                    disabled={isUpdatingParcel}
                                >
                                    Cancel
                                </button>
                            </div>
                        </div>
                    ) : null}

                    {!isDetailsLoading && !detailsError && !selectedParcel ? (
                        <div className="detail-block">
                            <p>Select a parcel to inspect details.</p>
                        </div>
                    ) : null}
                </section>
            </section>
        </div>
    );
}
