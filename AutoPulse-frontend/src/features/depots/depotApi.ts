import {apiRequest} from "@/api/httpClient";
import type {PageResponse} from "@/features/users/userServiceApi";

export interface DepotResponse {
    id: string;
    name: string;
    addressStreet: string;
    addressNumber: string;
    county: string;
    city: string;
    regionId: string;
    latitude: number | null;
    longitude: number | null;
    contactName: string;
    contactPhone: string;
    contactEmail: string;
    depotCode: string;
    active: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface DepotCoordinatesResponse {
    id: string;
    latitude: number;
    longitude: number;
}

export interface DepotPayload {
    name: string;
    addressStreet: string;
    addressNumber: string;
    county: string;
    city: string;
    contactName: string;
    contactPhone: string;
    contactEmail: string;
    depotCode: string;
    latitude: number;
    longitude: number;
}

export interface DepotQuery {
    page?: number;
    size?: number;
    sort?: string;
}

export function searchDepots(query: DepotQuery, signal?: AbortSignal) {
    return apiRequest<PageResponse<DepotResponse>>(`/fleet/depots${toQueryString({
        page: query.page ?? 0,
        size: query.size ?? 10,
        sort: query.sort
    })}`, {signal});
}

export function getDepot(depotId: string, signal?: AbortSignal) {
    return apiRequest<DepotResponse>(`/fleet/depots/${encodeURIComponent(depotId)}`, {signal});
}

export function createDepot(payload: DepotPayload) {
    return apiRequest<DepotResponse>("/fleet/depots", {
        method: "POST",
        body: payload
    });
}

export function updateDepot(depotId: string, payload: DepotPayload) {
    return apiRequest<DepotResponse>(`/fleet/depots/${encodeURIComponent(depotId)}`, {
        method: "PUT",
        body: payload
    });
}

export function activateDepot(depotId: string) {
    return apiRequest<DepotResponse>(`/fleet/depots/${encodeURIComponent(depotId)}/activate`, {method: "PATCH"});
}

export function deactivateDepot(depotId: string) {
    return apiRequest<DepotResponse>(`/fleet/depots/${encodeURIComponent(depotId)}/deactivate`, {method: "PATCH"});
}

export function getDepotCoordinates(depotId: string, signal?: AbortSignal) {
    return apiRequest<DepotCoordinatesResponse>(`/fleet/depots/${encodeURIComponent(depotId)}/coordinates`, {signal});
}

export function verifyDepotCode(depotCode: string, signal?: AbortSignal) {
    return apiRequest<boolean>(`/fleet/depots/verify_depot/${encodeURIComponent(depotCode)}`, {signal});
}

function toQueryString(params: Record<string, string | number | undefined>) {
    const searchParams = new URLSearchParams();

    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== "") {
            searchParams.set(key, String(value));
        }
    });

    const query = searchParams.toString();
    return query ? `?${query}` : "";
}
