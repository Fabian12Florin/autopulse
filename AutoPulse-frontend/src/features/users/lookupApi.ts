import {apiRequest} from "@/api/httpClient";
import type {PageResponse} from "@/features/users/userServiceApi";

export interface RegionResponse {
    id: string;
    name: string;
    code: string;
    createdAt: string;
    updatedAt: string;
}

export interface RegionReferenceResponse {
    id: string;
    name: string;
    code: string;
}

export interface DepotResponse {
    id: string;
    name: string;
    addressStreet: string;
    addressNumber: string;
    county: string;
    city: string;
    regionId: string;
    contactName: string;
    contactPhone: string;
    contactEmail: string;
    depotCode: string;
    latitude: number | null;
    longitude: number | null;
    active: boolean;
    createdAt: string;
    updatedAt: string;
}

export function searchRegions(query: {
    code?: string;
    name?: string;
    page?: number;
    size?: number
}, signal?: AbortSignal) {
    return apiRequest<PageResponse<RegionResponse>>(`/geography/query/regions${toQueryString({
        code: query.code,
        name: query.name,
        page: query.page ?? 0,
        size: query.size ?? 100
    })}`, {signal});
}

export function searchDepots(query: { page?: number; size?: number }, signal?: AbortSignal) {
    return apiRequest<PageResponse<DepotResponse>>(`/fleet/depots${toQueryString({
        page: query.page ?? 0,
        size: query.size ?? 500
    })}`, {signal});
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
