export function availabilityTone(status: "AVAILABLE" | "OFF_DUTY" | "SUSPENDED" | "ON_ROUTE") {
    if (status === "AVAILABLE") {
        return "success";
    }

    if (status === "SUSPENDED") {
        return "danger";
    }

    return "warning";
}
