import {apiRequest} from "@/api/httpClient";

export type AvailabilityStatus = "AVAILABLE" | "OFF_DUTY" | "SUSPENDED" | "ON_ROUTE";

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
    first: boolean;
    last: boolean;
    numberOfElements: number;
    empty: boolean;
}

export interface UserResponse {
    id: string;
    keycloakUserId: string;
    dispatcherProfileId: string | null;
    courierProfileId: string | null;
    email: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
    active: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface DispatcherResponse {
    dispatcherProfileId: string;
    regionCode: string;
    user: UserResponse;
    createdAt: string;
    updatedAt: string;
}

export interface CourierResponse {
    courierProfileId: string;
    depotId: string;
    regionCode: string;
    availabilityStatus: AvailabilityStatus;
    user: UserResponse;
    createdAt: string;
    updatedAt: string;
}

export interface DispatcherPayload {
    email: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
    regionCode: string;
    active?: boolean;
}

export interface CourierPayload extends DispatcherPayload {
    depotId: string;
    availabilityStatus: AvailabilityStatus;
}

export interface DispatcherQuery {
    regionCode?: string;
    active?: string;
    page: number;
    size: number;
}

export interface CourierQuery extends DispatcherQuery {
    depotId?: string;
    availabilityStatus?: string;
}

export interface UserQuery {
    active?: string;
    page: number;
    size: number;
}

export interface PasswordResetResponse {
    userId: string;
    email: string;
    message: string;
}

type CourierManagementScope = "admin" | "dispatcher";

export function searchDispatchers(query: DispatcherQuery, signal?: AbortSignal) {
    return apiRequest<PageResponse<DispatcherResponse>>(`/users/query/dispatchers${toQueryString(query)}`, {signal});
}

export function searchUsers(query: UserQuery, signal?: AbortSignal) {
    return apiRequest<PageResponse<UserResponse>>(`/users/query/users${toQueryString(query)}`, {signal});
}

export function getCurrentUser(signal?: AbortSignal) {
    return apiRequest<UserResponse>("/users/me", {signal});
}

export function createDispatcher(payload: DispatcherPayload) {
    return apiRequest<DispatcherResponse>("/users/admin/dispatchers", {
        method: "POST",
        body: withoutActive(payload)
    });
}

export function updateDispatcher(dispatcherProfileId: string, payload: Required<DispatcherPayload>) {
    return apiRequest<DispatcherResponse>(`/users/admin/dispatchers/${dispatcherProfileId}`, {
        method: "PATCH",
        body: payload
    });
}

export function searchCouriers(query: CourierQuery, scope: CourierManagementScope, signal?: AbortSignal) {
    const basePath = scope === "admin" ? "/users/query/couriers" : "/users/dispatcher/couriers";
    return apiRequest<PageResponse<CourierResponse>>(`${basePath}${toQueryString(query)}`, {signal});
}

export function getDispatcherProfile(dispatcherProfileId: string, signal?: AbortSignal) {
    return apiRequest<DispatcherResponse>(`/users/dispatcher/profiles/${dispatcherProfileId}`, {signal});
}

export function searchAvailableCouriers(query: CourierQuery, signal?: AbortSignal) {
    return apiRequest<PageResponse<CourierResponse>>(
        `/users/query/couriers${toQueryString({...query, availabilityStatus: "AVAILABLE", active: "true"})}`,
        {signal}
    );
}

export function getCourier(courierProfileId: string, signal?: AbortSignal) {
    return apiRequest<CourierResponse>(`/users/query/couriers/${courierProfileId}`, {signal});
}

export function createCourier(payload: CourierPayload, scope: CourierManagementScope) {
    const path = scope === "admin" ? "/users/admin/couriers" : "/users/dispatcher/couriers";
    return apiRequest<CourierResponse>(path, {
        method: "POST",
        body: withoutActive(payload)
    });
}

export function updateCourier(courierProfileId: string, payload: Required<CourierPayload>, scope: CourierManagementScope) {
    const path = scope === "admin"
        ? `/users/admin/couriers/${courierProfileId}`
        : `/users/dispatcher/couriers/${courierProfileId}`;

    return apiRequest<CourierResponse>(path, {
        method: "PATCH",
        body: payload
    });
}

export function updateMyCourierAvailability(availabilityStatus: AvailabilityStatus) {
    return apiRequest<CourierResponse>("/users/me/courier/availability", {
        method: "PATCH",
        body: {availabilityStatus}
    });
}

export function activateUser(userId: string) {
    return apiRequest<UserResponse>(`/users/admin/${userId}/activate`, {method: "PATCH"});
}

export function deactivateUser(userId: string) {
    return apiRequest<UserResponse>(`/users/admin/${userId}/deactivate`, {method: "PATCH"});
}

export function resetUserPassword(userId: string) {
    return apiRequest<PasswordResetResponse>(`/users/admin/${userId}/reset-password`, {method: "POST"});
}

function toQueryString(params: object) {
    const searchParams = new URLSearchParams();

    Object.entries(params).forEach(([key, value]) => {
        if ((typeof value === "string" || typeof value === "number") && value !== "") {
            searchParams.set(key, String(value));
        }
    });

    const query = searchParams.toString();
    return query ? `?${query}` : "";
}

function withoutActive<T extends { active?: boolean }>(payload: T) {
    const rest = {...payload};
    delete rest.active;
    return rest;
}
