import {apiRequest} from "@/api/httpClient";
import type {PageResponse} from "@/features/users/userServiceApi";

export type VehicleCategory = "M1" | "M2" | "M3";
export type FuelType = "PETROL" | "DIESEL" | "ELECTRIC" | "HYBRID" | "LPG";
export type VehicleStatus = "AVAILABLE" | "IN_USE" | "MAINTENANCE" | "OUT_OF_SERVICE";
export type VehicleAssignmentStatus = "ACTIVE" | "ENDED" | "CANCELLED";
export type VehicleDocumentType = "INSURANCE" | "VIGNETTE" | "PERIODIC_INSPECTION";

export interface VehicleResponse {
    id: string;
    depotId: string;
    licensePlate: string;
    brand: string;
    model: string;
    vin: string;
    year: number;
    category: VehicleCategory;
    fuelType: FuelType;
    capacityWeight: number;
    capacityVolume: number;
    status: VehicleStatus;
    createdAt: string;
    updatedAt: string;
}

export interface VehiclePayload {
    depotId: string;
    licensePlate: string;
    brand: string;
    model: string;
    vin: string;
    year: number;
    category: VehicleCategory;
    fuelType: FuelType;
    capacityWeight: number;
    capacityVolume: number;
}

export interface VehicleAssignmentResponse {
    id: string;
    vehicleId: string;
    courierId: string;
    assignedAt: string;
    unassignedAt: string | null;
    status: VehicleAssignmentStatus;
    createdAt: string;
    updatedAt: string;
}

export interface VehicleMaintenanceResponse {
    id: string;
    vehicleId: string;
    mileage: number;
    serviceDate: string;
    cost: number;
    description: string | null;
    serviceProvider: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface VehicleMaintenancePayload {
    mileage: number;
    serviceDate: string;
    cost: number;
    description: string;
    serviceProvider: string;
}

export interface VehicleDocumentResponse {
    id: string;
    vehicleId: string;
    documentType: VehicleDocumentType;
    issuedAt: string;
    expiresAt: string;
    cost: number;
    description: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface VehicleDocumentPayload {
    documentType: VehicleDocumentType;
    issuedAt: string;
    expiresAt: string;
    cost: number;
    description: string;
}

export interface VehicleQuery {
    page?: number;
    size?: number;
    sort?: string;
}

export function searchVehicles(query: VehicleQuery, signal?: AbortSignal) {
    return apiRequest<PageResponse<VehicleResponse>>(`/fleet/vehicles${toQueryString({
        page: query.page ?? 0,
        size: query.size ?? 10,
        sort: query.sort
    })}`, {signal});
}

export function createVehicle(payload: VehiclePayload) {
    return apiRequest<VehicleResponse>("/fleet/vehicles", {
        method: "POST",
        body: payload
    });
}

export function updateVehicle(vehicleId: string, payload: VehiclePayload) {
    return apiRequest<VehicleResponse>(`/fleet/vehicles/${encodeURIComponent(vehicleId)}`, {
        method: "PUT",
        body: payload
    });
}

export function updateVehicleStatus(vehicleId: string, status: VehicleStatus) {
    return apiRequest<VehicleResponse>(`/fleet/vehicles/${encodeURIComponent(vehicleId)}/status`, {
        method: "PATCH",
        body: {status}
    });
}

export function activateVehicle(vehicleId: string) {
    return apiRequest<VehicleResponse>(`/fleet/vehicles/${encodeURIComponent(vehicleId)}/activate`, {method: "PATCH"});
}

export function deactivateVehicle(vehicleId: string) {
    return apiRequest<VehicleResponse>(`/fleet/vehicles/${encodeURIComponent(vehicleId)}/deactivate`, {method: "PATCH"});
}

export function searchVehicleAssignments(query: VehicleQuery, signal?: AbortSignal) {
    return apiRequest<PageResponse<VehicleAssignmentResponse>>(`/fleet/vehicle-assignments${toQueryString({
        page: query.page ?? 0,
        size: query.size ?? 500,
        sort: query.sort
    })}`, {signal});
}

export function assignVehicle(vehicleId: string, courierId: string) {
    return apiRequest<VehicleAssignmentResponse>("/fleet/vehicle-assignments", {
        method: "POST",
        body: {vehicleId, courierId}
    });
}

export function endVehicleAssignment(assignmentId: string) {
    return apiRequest<VehicleAssignmentResponse>(
        `/fleet/vehicle-assignments/${encodeURIComponent(assignmentId)}/end`,
        {method: "PATCH"}
    );
}

export function searchVehicleMaintenanceRecords(vehicleId: string, query: VehicleQuery, signal?: AbortSignal) {
    return apiRequest<PageResponse<VehicleMaintenanceResponse>>(
        `/fleet/vehicles/${encodeURIComponent(vehicleId)}/maintenance-records${toQueryString({
            page: query.page ?? 0,
            size: query.size ?? 10,
            sort: query.sort
        })}`,
        {signal}
    );
}

export function createVehicleMaintenanceRecord(vehicleId: string, payload: VehicleMaintenancePayload) {
    return apiRequest<VehicleMaintenanceResponse>(
        `/fleet/vehicles/${encodeURIComponent(vehicleId)}/maintenance-records`,
        {
            method: "POST",
            body: payload
        }
    );
}

export function updateVehicleMaintenanceRecord(recordId: string, payload: VehicleMaintenancePayload) {
    return apiRequest<VehicleMaintenanceResponse>(`/fleet/maintenance-records/${encodeURIComponent(recordId)}`, {
        method: "PUT",
        body: payload
    });
}

export function deleteVehicleMaintenanceRecord(recordId: string) {
    return apiRequest<void>(`/fleet/maintenance-records/${encodeURIComponent(recordId)}`, {method: "DELETE"});
}

export function searchVehicleDocuments(vehicleId: string, query: VehicleQuery, signal?: AbortSignal) {
    return apiRequest<PageResponse<VehicleDocumentResponse>>(
        `/fleet/vehicles/${encodeURIComponent(vehicleId)}/documents${toQueryString({
            page: query.page ?? 0,
            size: query.size ?? 10,
            sort: query.sort
        })}`,
        {signal}
    );
}

export function createVehicleDocument(vehicleId: string, payload: VehicleDocumentPayload) {
    return apiRequest<VehicleDocumentResponse>(
        `/fleet/vehicles/${encodeURIComponent(vehicleId)}/documents`,
        {
            method: "POST",
            body: payload
        }
    );
}

export function updateVehicleDocument(documentId: string, payload: VehicleDocumentPayload) {
    return apiRequest<VehicleDocumentResponse>(`/fleet/documents/${encodeURIComponent(documentId)}`, {
        method: "PUT",
        body: payload
    });
}

export function deleteVehicleDocument(documentId: string) {
    return apiRequest<void>(`/fleet/documents/${encodeURIComponent(documentId)}`, {method: "DELETE"});
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
