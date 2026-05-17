import {apiRequest} from "@/api/httpClient";
import type {PageResponse} from "@/features/users/userServiceApi";

export interface RegionReferenceResponse {
    id: string;
    name: string;
    code: string;
}

export interface RegionResponse extends RegionReferenceResponse {
    createdAt: string;
    updatedAt: string;
}

export interface CountyReferenceResponse {
    id: string;
    name: string;
    region: RegionReferenceResponse;
}

export interface CountyResponse extends CountyReferenceResponse {
    createdAt: string;
    updatedAt: string;
}

export type LocalityReferenceResponse = CountyReferenceResponse;
export type LocalityResponse = CountyResponse;

export interface RegionPayload {
    name: string;
    code: string;
}

export interface CountyPayload {
    name: string;
    regionCode: string;
}

export type LocalityPayload = CountyPayload;

export interface CoordinatesResponse {
    x: number;
    y: number;
}

export interface GeocodeAddressPayload {
    street: string;
    city: string;
    county?: string;
}

export interface RegionQuery {
    name?: string;
    code?: string;
    page?: number;
    size?: number;
}

export interface CountyQuery {
    name?: string;
    regionId?: string;
    regionCode?: string;
    page?: number;
    size?: number;
}

export function searchRegions(query: RegionQuery, signal?: AbortSignal) {
    return apiRequest<PageResponse<RegionResponse>>(`/geography/query/regions${toQueryString({
        name: query.name,
        code: query.code,
        page: query.page ?? 0,
        size: query.size ?? 20
    })}`, {signal});
}

export function getRegion(regionId: string, signal?: AbortSignal) {
    return apiRequest<RegionResponse>(`/geography/query/regions/${regionId}`, {signal});
}

export function getRegionByCode(code: string, signal?: AbortSignal) {
    return apiRequest<RegionResponse>(`/geography/query/regions/by-code/${encodeURIComponent(code)}`, {signal});
}

export function createRegion(payload: RegionPayload) {
    return apiRequest<RegionResponse>("/geography/admin/regions", {
        method: "POST",
        body: payload
    });
}

export function searchCounties(query: CountyQuery, signal?: AbortSignal) {
    return apiRequest<PageResponse<CountyResponse>>(`/geography/query/counties${toQueryString({
        name: query.name,
        regionId: query.regionId,
        regionCode: query.regionCode,
        page: query.page ?? 0,
        size: query.size ?? 20
    })}`, {signal});
}

export function searchLocalities(query: CountyQuery, signal?: AbortSignal) {
    return searchCounties(query, signal);
}

export function createCounty(payload: CountyPayload) {
    return apiRequest<CountyResponse>("/geography/admin/counties", {
        method: "POST",
        body: payload
    });
}

export function createLocality(payload: CountyPayload) {
    return createCounty(payload);
}

export function resolveCoordinates(payload: GeocodeAddressPayload) {
    return apiRequest<CoordinatesResponse>("/geography/query/coordinates", {
        method: "POST",
        body: payload
    });
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
