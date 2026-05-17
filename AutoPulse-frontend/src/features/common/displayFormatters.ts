interface DepotLike {
    id: string;
    name?: string | null;
    depotCode?: string | null;
    code?: string | null;
}

interface CourierLike {
    courierProfileId?: string | null;
    user?: {
        firstName?: string | null;
        lastName?: string | null;
    } | null;
}

interface VehicleLike {
    licensePlate?: string | null;
    brand?: string | null;
    model?: string | null;
}

export function shortReference(value: string | null | undefined, size = 8) {
    const text = (value ?? "").trim();
    if (!text) {
        return "-";
    }
    return text.length <= size ? text : `${text.slice(0, size)}...`;
}

export function routeLabel(routeId: string | null | undefined) {
    // return `${shortReference(routeId)}`;
    return routeId;
}

export function runLabel(runId: string | null | undefined) {
    // return `${shortReference(runId)}`;
    return runId;
}

export function userLabel(userId: string | null | undefined, prefix = "User") {
    return `${prefix} ${shortReference(userId)}`;
}

export function depotLabelFromMap(depotId: string | null | undefined, depotById: Map<string, DepotLike>) {
    if (!depotId) {
        return "-";
    }

    const depot = depotById.get(depotId);
    if (!depot) {
        return `Depot ${shortReference(depotId)}`;
    }

    return depotLabel(depot);
}

export function depotLabel(depot: DepotLike | null | undefined) {
    if (!depot) {
        return "-";
    }

    const code = (depot.depotCode ?? depot.code ?? "").trim();
    const name = (depot.name ?? "").trim();

    if (code && name) {
        return `${code} (${name})`;
    }
    if (name) {
        return name;
    }
    if (code) {
        return code;
    }
    return `Depot ${shortReference(depot.id)}`;
}

export function courierLabel(courier: CourierLike | null | undefined, fallbackId?: string | null) {
    const firstName = courier?.user?.firstName?.trim() ?? "";
    const lastName = courier?.user?.lastName?.trim() ?? "";
    const fullName = `${firstName} ${lastName}`.trim();
    if (fullName) {
        return fullName;
    }

    const profileId = courier?.courierProfileId ?? fallbackId ?? null;
    return `Courier ${shortReference(profileId)}`;
}

export function vehicleLabel(vehicle: VehicleLike | null | undefined, fallbackId?: string | null) {
    if (!vehicle) {
        return `Vehicle ${shortReference(fallbackId)}`;
    }

    const licensePlate = vehicle.licensePlate?.trim() ?? "";
    const model = `${vehicle.brand ?? ""} ${vehicle.model ?? ""}`.trim();
    if (licensePlate && model) {
        return `${licensePlate} (${model})`;
    }
    if (licensePlate) {
        return licensePlate;
    }
    if (model) {
        return model;
    }
    return `Vehicle ${shortReference(fallbackId)}`;
}
