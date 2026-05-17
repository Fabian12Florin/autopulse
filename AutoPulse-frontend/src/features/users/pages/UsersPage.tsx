import {type Dispatch, type FormEvent, type ReactNode, type SetStateAction, useEffect, useMemo, useState} from "react";
import {PaginationControls} from "@/components/PaginationControls";
import {PageFeedback} from "@/components/PageFeedback";
import {PageHeader} from "@/components/PageHeader";
import {Tag} from "@/components/Tag";
import {loadAllPages} from "@/features/common/loadAllPages";
import {errorMessage} from "@/features/common/pageUtils";
import {type RegionResponse, searchRegions} from "@/features/users/lookupApi";
import {
    activateUser,
    createDispatcher,
    deactivateUser,
    type DispatcherPayload,
    type DispatcherResponse,
    type PageResponse,
    resetUserPassword,
    searchDispatchers,
    updateDispatcher
} from "@/features/users/userServiceApi";

type ActiveFilter = "" | "true" | "false";
type DispatcherFormValues = Required<DispatcherPayload>;

const PAGE_SIZE = 10;

const emptyDispatcherForm: DispatcherFormValues = {
    email: "",
    firstName: "",
    lastName: "",
    phoneNumber: "",
    regionCode: "",
    active: true
};

export function UsersPage() {
    const [dispatcherPage, setDispatcherPage] = useState<PageResponse<DispatcherResponse> | null>(null);
    const [dispatcherFilters, setDispatcherFilters] = useState({
        regionCode: "",
        active: "" as ActiveFilter,
        search: ""
    });
    const [dispatcherPageNumber, setDispatcherPageNumber] = useState(0);
    const [selectedDispatcherId, setSelectedDispatcherId] = useState<string | null>(null);
    const [creating, setCreating] = useState(false);
    const [dispatcherForm, setDispatcherForm] = useState<DispatcherFormValues>(emptyDispatcherForm);
    const [isLoading, setIsLoading] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [notice, setNotice] = useState<string | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);
    const [regions, setRegions] = useState<RegionResponse[]>([]);
    const [regionLookupError, setRegionLookupError] = useState<string | null>(null);

    useEffect(() => {
        const controller = new AbortController();
        setIsLoading(true);
        setError(null);

        searchDispatchers(
            {
                page: dispatcherPageNumber,
                size: PAGE_SIZE,
                regionCode: dispatcherFilters.regionCode.trim(),
                active: dispatcherFilters.active
            },
            controller.signal
        )
            .then(setDispatcherPage)
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setError(errorMessage(loadError, "Could not load dispatchers."));
                }
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsLoading(false);
                }
            });

        return () => controller.abort();
    }, [dispatcherFilters.active, dispatcherFilters.regionCode, dispatcherPageNumber, refreshKey]);

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
            .catch((loadError) => {
                if (!controller.signal.aborted) {
                    setRegionLookupError(errorMessage(loadError, "Could not load regions."));
                }
            });

        return () => controller.abort();
    }, []);

    const filteredDispatchers = useMemo(
        () => (dispatcherPage?.content ?? []).filter((dispatcher) => matchesProfileSearch(dispatcher.user, dispatcherFilters.search)),
        [dispatcherFilters.search, dispatcherPage]
    );

    const selectedDispatcher = useMemo(
        () => dispatcherPage?.content.find((dispatcher) => dispatcher.dispatcherProfileId === selectedDispatcherId) ?? null,
        [dispatcherPage, selectedDispatcherId]
    );
    const filterRegionOptions = useMemo(
        () => ensureRegionOption(regions, dispatcherFilters.regionCode),
        [dispatcherFilters.regionCode, regions]
    );
    const formRegionOptions = useMemo(
        () => ensureRegionOption(regions, dispatcherForm.regionCode),
        [dispatcherForm.regionCode, regions]
    );

    useEffect(() => {
        if (creating) {
            setDispatcherForm(emptyDispatcherForm);
            return;
        }

        if (selectedDispatcher) {
            setDispatcherForm(toDispatcherForm(selectedDispatcher));
            return;
        }

        if (dispatcherPage?.content.length) {
            setSelectedDispatcherId(dispatcherPage.content[0].dispatcherProfileId);
        }
    }, [creating, dispatcherPage, selectedDispatcher]);

    async function handleDispatcherSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setNotice(null);

        try {
            const validationError = validateDispatcherPayload(dispatcherForm);
            if (validationError) {
                setError(validationError);
                return;
            }

            if (!creating && !selectedDispatcher) {
                setError("Select a dispatcher before saving changes.");
                return;
            }

            setIsSaving(true);

            if (creating) {
                const created = await createDispatcher(dispatcherForm);
                setSelectedDispatcherId(created.dispatcherProfileId);
                setCreating(false);
                setNotice("Dispatcher created.");
            } else if (selectedDispatcher) {
                await updateDispatcher(selectedDispatcher.dispatcherProfileId, dispatcherForm);
                setNotice("Dispatcher profile updated.");
            }
            refresh();
        } catch (saveError) {
            setError(errorMessage(saveError, "Could not save dispatcher."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handleLifecycle(entity: DispatcherResponse) {
        setIsSaving(true);
        setError(null);
        setNotice(null);

        try {
            const nextUser = entity.user.active ? await deactivateUser(entity.user.id) : await activateUser(entity.user.id);
            setNotice(`${fullName(nextUser)} is now ${nextUser.active ? "active" : "inactive"}.`);
            refresh();
        } catch (lifecycleError) {
            setError(errorMessage(lifecycleError, "Could not update user state."));
        } finally {
            setIsSaving(false);
        }
    }

    async function handlePasswordReset(entity: DispatcherResponse) {
        setIsSaving(true);
        setError(null);
        setNotice(null);

        try {
            const response = await resetUserPassword(entity.user.id);
            setNotice(response.message || `Password reset accepted for ${response.email}.`);
        } catch (resetError) {
            setError(errorMessage(resetError, "Could not reset password."));
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
                title="Dispatchers"
                subtitle="Admin directory for dispatcher profiles, lifecycle state, and password reset flows."
                actions={<Tag text="Admin" tone="success"/>}
            />

            <section className="toolbar-row">
                <button
                    type="button"
                    className="secondary-btn"
                    onClick={() => {
                        setCreating(true);
                        setNotice(null);
                        setError(null);
                    }}
                >
                    New dispatcher
                </button>
            </section>

            <PageFeedback error={error} notice={notice} errors={regionLookupError ? [regionLookupError] : []}/>

            <section className="two-column-grid directory-grid">
                <DirectoryList
                    title="Dispatchers"
                    total={dispatcherPage?.totalElements ?? 0}
                    loading={isLoading}
                    filters={(
                        <div className="filter-grid">
                            <label>
                                Region
                                <select
                                    value={dispatcherFilters.regionCode}
                                    onChange={(event) => {
                                        setDispatcherPageNumber(0);
                                        setDispatcherFilters((current) => ({
                                            ...current,
                                            regionCode: event.target.value
                                        }));
                                    }}
                                >
                                    <option value="">Any region</option>
                                    {filterRegionOptions.map((region) => (
                                        <option key={region.id} value={region.code}>
                                            {region.code} - {region.name}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <label>
                                State
                                <select
                                    value={dispatcherFilters.active}
                                    onChange={(event) => {
                                        setDispatcherPageNumber(0);
                                        setDispatcherFilters((current) => ({
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
                                    value={dispatcherFilters.search}
                                    onChange={(event) => setDispatcherFilters((current) => ({
                                        ...current,
                                        search: event.target.value
                                    }))}
                                    placeholder="Search loaded page"
                                />
                            </label>
                        </div>
                    )}
                    columns={["Name", "Region", "Phone", "State"]}
                    rows={filteredDispatchers.map((dispatcher) => [
                        <button
                            key={dispatcher.dispatcherProfileId}
                            type="button"
                            className={dispatcher.dispatcherProfileId === selectedDispatcherId ? "text-link active-row" : "text-link"}
                            onClick={() => {
                                setCreating(false);
                                setSelectedDispatcherId(dispatcher.dispatcherProfileId);
                            }}
                        >
                            {fullName(dispatcher.user)}
                        </button>,
                        dispatcher.regionCode,
                        dispatcher.user.phoneNumber,
                        <Tag
                            key={`${dispatcher.dispatcherProfileId}-state`}
                            text={dispatcher.user.active ? "ACTIVE" : "INACTIVE"}
                            tone={dispatcher.user.active ? "success" : "warning"}
                        />
                    ])}
                    pagination={
                        dispatcherPage ? (
                            <PaginationControls page={dispatcherPage} onPageChange={setDispatcherPageNumber}/>
                        ) : null
                    }
                />

                <section className="table-card">
                    <div className="table-card-head">
                        <h3>{creating ? "New Dispatcher" : "Dispatcher Details"}</h3>
                        <p>Identity fields, region assignment, and lifecycle state</p>
                    </div>
                    <form className="detail-form" onSubmit={handleDispatcherSubmit} noValidate>
                        <ProfileFields form={dispatcherForm} onChange={setDispatcherForm}/>
                        <label>
                            Region
                            <select
                                value={dispatcherForm.regionCode}
                                onChange={(event) => setDispatcherForm((current) => ({
                                    ...current,
                                    regionCode: event.target.value
                                }))}
                                required
                            >
                                <option value="">Select region</option>
                                {formRegionOptions.map((region) => (
                                    <option key={region.id} value={region.code}>
                                        {region.code} - {region.name}
                                    </option>
                                ))}
                            </select>
                        </label>
                        <ActiveCheckbox
                            checked={dispatcherForm.active}
                            onChange={(active) => setDispatcherForm((current) => ({...current, active}))}
                        />
                        <DetailActions
                            canSave={creating || Boolean(selectedDispatcher)}
                            canUseLifecycle={Boolean(selectedDispatcher && !creating)}
                            isActive={selectedDispatcher?.user.active}
                            isSaving={isSaving}
                            onLifecycle={() => selectedDispatcher && handleLifecycle(selectedDispatcher)}
                            onPasswordReset={() => selectedDispatcher && handlePasswordReset(selectedDispatcher)}
                        />
                    </form>
                </section>
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
                           pagination
                       }: {
    title: string;
    total: number;
    loading: boolean;
    filters: ReactNode;
    columns: string[];
    rows: ReactNode[][];
    pagination: ReactNode;
}) {
    return (
        <section className={`table-card${loading ? " loading" : ""}`}>
            <div className="table-card-head">
                <h3>{title}</h3>
                <p>{loading ? "Loading..." : `${total} profiles found`}</p>
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
                            <tr key={`directory-row-${rowIndex}`}>
                                {row.map((cell, cellIndex) => (
                                    <td key={`directory-cell-${rowIndex}-${cellIndex}`}>{cell}</td>
                                ))}
                            </tr>
                        ))
                    ) : (
                        <tr>
                            <td colSpan={columns.length}>No profiles match the current filters.</td>
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

function ProfileFields({
                           form,
                           onChange
                       }: {
    form: DispatcherFormValues;
    onChange: Dispatch<SetStateAction<DispatcherFormValues>>;
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

function ActiveCheckbox({checked, onChange}: { checked: boolean; onChange: (checked: boolean) => void }) {
    return (
        <label className="checkbox-label">
            <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)}/>
            Active
        </label>
    );
}

function DetailActions({
                           canSave,
                           canUseLifecycle,
                           isActive,
                           isSaving,
                           onLifecycle,
                           onPasswordReset
                       }: {
    canSave: boolean;
    canUseLifecycle: boolean;
    isActive: boolean | undefined;
    isSaving: boolean;
    onLifecycle: () => void;
    onPasswordReset: () => void;
}) {
    return (
        <div className="action-row">
            <button type="submit" className="primary-btn" disabled={!canSave || isSaving}>
                {isSaving ? "Saving..." : "Save profile"}
            </button>
            <button type="button" className="secondary-btn" onClick={onLifecycle}
                    disabled={!canUseLifecycle || isSaving}>
                {isActive ? "Deactivate" : "Activate"}
            </button>
            <button
                type="button"
                className="secondary-btn"
                onClick={onPasswordReset}
                disabled={!canUseLifecycle || isSaving}
            >
                Reset password
            </button>
        </div>
    );
}

function toDispatcherForm(dispatcher: DispatcherResponse): DispatcherFormValues {
    return {
        email: dispatcher.user.email,
        firstName: dispatcher.user.firstName,
        lastName: dispatcher.user.lastName,
        phoneNumber: dispatcher.user.phoneNumber,
        regionCode: dispatcher.regionCode,
        active: dispatcher.user.active
    };
}

function validateDispatcherPayload(payload: DispatcherFormValues) {
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

    if (!payload.regionCode.trim()) {
        return "Region is required.";
    }

    return null;
}

function matchesProfileSearch(user: { firstName: string; lastName: string; email: string }, search: string) {
    const term = search.trim().toLowerCase();
    if (!term) {
        return true;
    }

    return `${user.firstName} ${user.lastName} ${user.email}`.toLowerCase().includes(term);
}

function fullName(user: { firstName: string; lastName: string }) {
    return `${user.firstName} ${user.lastName}`.trim();
}

function ensureRegionOption(regions: RegionResponse[], selectedCode: string) {
    const code = selectedCode.trim();
    if (!code || regions.some((region) => region.code === code)) {
        return regions;
    }

    return [
        ...regions,
        {
            id: `__custom_${code}`,
            code,
            name: "Unknown region",
            createdAt: "",
            updatedAt: ""
        }
    ];
}
