import {apiRequest} from "@/api/httpClient";
import type {PageResponse} from "@/features/users/userServiceApi";

export type RoutingRouteType = "SENDER_TO_REGIONAL" | "REGIONAL_TO_REGIONAL" | "REGIONAL_TO_RECEIVER";
export type RoutingJobStatus = "PENDING" | "SELECTED";

export interface RoutingJobResponse {
    id: string;
    generatedByUserId: string;
    depotId: string;
    routeDate: string;
    routeType: RoutingRouteType;
    routingJobStatus: RoutingJobStatus;
    inputParcelCount: number;
    inputCourierCount: number;
    inputVehicleCount: number;
    assignedParcelCount: number;
    unassignedParcelCount: number;
    numberOfRoutes: number;
}

export interface RoutingJobQuery {
    depotCode?: string;
    routeDate?: string;
    routeType?: RoutingRouteType;
    status?: RoutingJobStatus;
    page?: number;
    size?: number;
    sort?: string;
}

export interface RoutingJobCreatePayload {
    depotCode: string;
    routeDate: string;
    routeType: RoutingRouteType;
}

export function queryRoutingJobs(query: RoutingJobQuery, signal?: AbortSignal) {
    return apiRequest<PageResponse<RoutingJobResponse>>(`/routing/routing-jobs${toQueryString({
        depotCode: query.depotCode,
        routeDate: query.routeDate,
        routeType: query.routeType,
        status: query.status,
        page: query.page ?? 0,
        size: query.size ?? 10,
        sort: query.sort
    })}`, {signal});
}

export function createRoutingJob(payload: RoutingJobCreatePayload) {
    return apiRequest<RoutingJobResponse>("/routing/routing-jobs/create", {
        method: "POST",
        body: payload
    });
}

export function selectRoutingJob(routingJobId: string) {
    return apiRequest<RoutingJobResponse>(`/routing/routing-jobs/${encodeURIComponent(routingJobId)}/select`, {
        method: "POST"
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
