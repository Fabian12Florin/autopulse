export interface Metric {
    label: string;
    value: string;
    trend: string;
    tone: "positive" | "negative" | "neutral" | "warning";
}

export const dashboardMetrics: Metric[] = [
    {label: "Parcels in transit", value: "18,422", trend: "+5.4% today", tone: "positive"},
    {label: "Failed deliveries", value: "213", trend: "-1.1% vs yesterday", tone: "positive"},
    {label: "Active couriers", value: "746", trend: "+12 new this week", tone: "neutral"},
    {label: "Vehicles unavailable", value: "17", trend: "+2 pending maintenance", tone: "warning"}
];

export const parcelRows = [
    ["AP-100932", "Bucharest", "Out for delivery", "Courier-198", "10:40"],
    ["AP-100911", "Brasov", "At depot", "Assigned", "09:05"],
    ["AP-100875", "Cluj", "Delivery failed", "Courier-087", "08:12"]
];

export const routeRows = [
    ["R-301", "Depot North", "42", "0h 34m", "Published"],
    ["R-302", "Depot East", "38", "0h 22m", "Needs approval"],
    ["R-303", "Depot South", "44", "0h 40m", "Published"]
];
