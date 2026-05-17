import {apiRequest} from "@/api/httpClient";
import type {PageResponse} from "@/features/users/userServiceApi";

export type DeliveryRunStatus = "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED" | "ABORTED";
export type DeliveryStopStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED";
export type DeliveryOutcome = "DELIVERED" | "FAILED" | "REJECTED" | "WAITING_PICKUP";

export interface DeliveryRunResponse {
    id: string;
    routeId: string;
    courierId: string;
    vehicleId: string;
    routeDate: string;
    googleMapsUrl: string;
    totalParcelCount: number;
    totalWeight: number;
    totalVolume: number;
    status: DeliveryRunStatus;
    startedAt: string | null;
    finishedAt: string | null;
}

export interface DeliveryParcelExecutionResponse {
    id: string;
    parcelId: string;
    awb: string;
    receiverName: string;
    receiverPhone: string;
    weight: number | null;
    volume: number | null;
    outcome: DeliveryOutcome | null;
    deliveryCodeVerified: boolean;
    completedAt: string | null;
}

export interface DeliveryStopExecutionResponse {
    id: string;
    routeStopId: string;
    stopOrder: number;
    latitude: number;
    longitude: number;
    parcelCount: number;
    totalWeight: number;
    totalVolume: number;
    status: DeliveryStopStatus;
    parcels: DeliveryParcelExecutionResponse[];
}

export interface DeliveryRunDetailsResponse {
    id: string;
    routeId: string;
    courierId: string;
    vehicleId: string;
    routeDate: string;
    googleMapsUrl: string;
    totalParcelCount: number;
    totalWeight: number;
    totalVolume: number;
    status: DeliveryRunStatus;
    currentStop: DeliveryStopExecutionResponse | null;
    stops: DeliveryStopExecutionResponse[];
}

export interface DeliveryRunQuery {
    courierId?: string;
    routeDate?: string;
    status?: DeliveryRunStatus;
    page?: number;
    size?: number;
    sort?: string;
}

export interface VerifyDeliveryCodePayload {
    awb: string;
    pin: string;
}

export interface CompleteParcelDeliveryPayload {
    outcome: DeliveryOutcome;
    notes?: string;
    paymentCollected?: number;
}

export function queryDeliveryRuns(query: DeliveryRunQuery, signal?: AbortSignal) {
    return apiRequest<PageResponse<DeliveryRunResponse>>(
        `/delivery-execution/runs${toQueryString({
            courierId: query.courierId,
            routeDate: query.routeDate,
            status: query.status,
            page: query.page ?? 0,
            size: query.size ?? 10,
            sort: query.sort
        })}`,
        {signal}
    );
}

export function getDeliveryRunDetails(deliveryRunId: string, signal?: AbortSignal) {
    return apiRequest<DeliveryRunDetailsResponse>(`/delivery-execution/runs/${encodeURIComponent(deliveryRunId)}`, {signal});
}

export function startDeliveryRun(deliveryRunId: string) {
    return apiRequest<DeliveryRunResponse>(`/delivery-execution/runs/${encodeURIComponent(deliveryRunId)}/start`, {
        method: "POST"
    });
}

export function abortDeliveryRun(deliveryRunId: string) {
    return apiRequest<DeliveryRunResponse>(`/delivery-execution/runs/${encodeURIComponent(deliveryRunId)}/abort`, {
        method: "POST"
    });
}

export function completeDeliveryRun(deliveryRunId: string) {
    return apiRequest<DeliveryRunResponse>(`/delivery-execution/runs/${encodeURIComponent(deliveryRunId)}/complete`, {
        method: "POST"
    });
}

export function arriveAtDeliveryStop(deliveryRunId: string, deliveryStopExecutionId: string) {
    return apiRequest<DeliveryRunDetailsResponse>(
        `/delivery-execution/runs/${encodeURIComponent(deliveryRunId)}/stops/${encodeURIComponent(deliveryStopExecutionId)}/arrive`,
        {
            method: "POST"
        }
    );
}

export function verifyDeliveryCode(
    deliveryRunId: string,
    deliveryStopExecutionId: string,
    payload: VerifyDeliveryCodePayload
) {
    return apiRequest<DeliveryParcelExecutionResponse>(
        `/delivery-execution/runs/${encodeURIComponent(deliveryRunId)}/stops/${encodeURIComponent(deliveryStopExecutionId)}/parcels/verify-code`,
        {
            method: "POST",
            body: payload
        }
    );
}

export function completeDeliveryParcel(
    deliveryRunId: string,
    deliveryParcelExecutionId: string,
    payload: CompleteParcelDeliveryPayload
) {
    return apiRequest<DeliveryParcelExecutionResponse>(
        `/delivery-execution/runs/${encodeURIComponent(deliveryRunId)}/parcels/${encodeURIComponent(deliveryParcelExecutionId)}/complete`,
        {
            method: "POST",
            body: payload
        }
    );
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
