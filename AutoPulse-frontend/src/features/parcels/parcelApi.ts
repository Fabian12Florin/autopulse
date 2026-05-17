import {apiRequest} from "@/api/httpClient";

export type ParcelBackendStatus =
    | "CREATED"
    | "SENDER_REGIONAL_COURIER_ASSIGNED"
    | "PICKED_UP_BY_SENDER_REGIONAL_COURIER"
    | "IN_SENDER_REGIONAL_DEPOSIT"
    | "TRANSIT_COURIER_ASSIGNED"
    | "IN_TRANSIT"
    | "IN_RECEIVER_REGIONAL_DEPOSIT"
    | "RECEIVER_REGIONAL_COURIER_ASSIGNED"
    | "IN_DELIVERY"
    | "DELIVERED"
    | "WAITING_IN_DEPOT";

export interface ParcelSummary {
    id: string;
    awb: string;
    depotCode: string;
    receiverName: string;
    receiverCity: string;
    receiverCounty: string;
    weight: number;
    volume: number;
    status: ParcelBackendStatus;
    createdAt: string;
}

export interface ParcelContactAddress {
    county: string;
    city: string;
    street: string;
    number: string;
    longitude: number;
    latitude: number;
}

export interface ParcelContact {
    name: string;
    email: string | null;
    phone: string;
    address: ParcelContactAddress;
}

export interface ParcelDetails {
    id: string;
    awb: string;
    depotCode: string;
    senderContact: ParcelContact;
    receiverContact: ParcelContact;
    weight: number;
    volume: number;
    status: ParcelBackendStatus;
    paymentRequired: boolean;
    paymentAmount: number | string | null;
    declaredValue: number | string | null;
    observations: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface ParcelPin {
    pin: string;
}

export interface DeliveryCodeVerification {
    valid: boolean;
}

export interface ParcelContactAddressUpdate {
    county: string;
    city: string;
    street: string;
    number: string;
}

export interface ParcelContactUpdate {
    name: string;
    email?: string | null;
    phone: string;
    address: ParcelContactAddressUpdate;
}

export interface UpdateParcelRequest {
    depotCode?: string;
    senderContact?: ParcelContactUpdate;
    receiverContact?: ParcelContactUpdate;
    weight?: number;
    volume?: number;
    paymentRequired?: boolean;
    paymentAmount?: number;
    declaredValue?: number;
    observations?: string;
}

export interface CreateParcelRequest {
    senderContact: ParcelContactUpdate;
    receiverContact: ParcelContactUpdate;
    weight: number;
    volume: number;
    paymentRequired: boolean;
    paymentAmount?: number;
    declaredValue?: number;
    observations?: string;
}

export interface ParcelPageResult {
    content: ParcelSummary[];
    totalElements: number;
}

export interface DepotOption {
    id: string;
    code: string;
    name: string;
}

interface PageResponse<T> {
    content: T[];
    totalElements: number;
}

interface DepotResponseDto {
    id: string;
    name: string;
    depotCode?: string;
    code?: string;
}

interface ParcelQuery {
    depotCode?: string;
    awb?: string;
    status?: ParcelBackendStatus;
    page?: number;
    size?: number;
}

export async function searchParcels(query: ParcelQuery): Promise<ParcelPageResult> {
    const params = new URLSearchParams();

    if (query.depotCode) {
        params.set("depotCode", query.depotCode);
    }
    if (query.awb) {
        params.set("awb", query.awb);
    }
    if (query.status) {
        params.set("status", query.status);
    }
    params.set("page", String(query.page ?? 0));
    params.set("size", String(query.size ?? 200));
    params.set("sortBy", "createdAt");
    params.set("sortDirection", "DESC");

    const response = await apiRequest<PageResponse<ParcelSummary>>(`/parcels?${params.toString()}`);
    return {
        content: response.content ?? [],
        totalElements: response.totalElements ?? 0
    };
}

export function getParcel(parcelId: string) {
    return apiRequest<ParcelDetails>(`/parcels/${parcelId}`);
}

export function getParcelByAwb(awb: string) {
    return apiRequest<ParcelDetails>(`/parcels/awb/${encodeURIComponent(awb)}`);
}

export function getParcelPinByAwb(awb: string) {
    return apiRequest<ParcelPin>(`/parcels/awb/${encodeURIComponent(awb)}/pin`);
}

export function verifyDeliveryCode(awb: string, pin: string) {
    return apiRequest<DeliveryCodeVerification>("/parcels/verify-delivery-code", {
        method: "POST",
        body: {awb, pin}
    });
}

export function updateParcelStatus(parcelId: string, status: ParcelBackendStatus) {
    return apiRequest<void>(`/parcels/${parcelId}/status`, {
        method: "POST",
        body: {status}
    });
}

export function updateParcel(parcelId: string, payload: UpdateParcelRequest) {
    return apiRequest<ParcelDetails>(`/parcels/${parcelId}`, {
        method: "PUT",
        body: payload
    });
}

export function createParcel(payload: CreateParcelRequest) {
    return apiRequest<ParcelDetails>("/parcels", {
        method: "POST",
        body: payload
    });
}

export async function getDepotOptions(): Promise<DepotOption[]> {
    const response = await apiRequest<
        PageResponse<DepotResponseDto> | DepotResponseDto[] | { items?: DepotResponseDto[] }
    >(
        "/fleet/depots?page=0&size=10"
    );

    let rows: DepotResponseDto[] = [];
    if (Array.isArray(response)) {
        rows = response;
    } else if ("content" in response && Array.isArray(response.content)) {
        rows = response.content;
    } else if ("items" in response && Array.isArray(response.items)) {
        rows = response.items;
    }

    return rows
        .map((entry) => ({
            id: String(entry.id),
            code: (entry.depotCode ?? entry.code ?? "").trim(),
            name: (entry.name ?? "").trim()
        }))
        .filter((entry) => entry.code.length > 0);
}
