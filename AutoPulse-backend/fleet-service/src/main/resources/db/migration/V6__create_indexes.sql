CREATE INDEX idx_depots_locality_id ON depots(locality_id);
CREATE INDEX idx_depots_active ON depots(active);

CREATE INDEX idx_vehicles_depot_id ON vehicles(depot_id);
CREATE INDEX idx_vehicles_status ON vehicles(status);
CREATE INDEX idx_vehicles_category ON vehicles(category);
CREATE INDEX idx_vehicles_fuel_type ON vehicles(fuel_type);

CREATE INDEX idx_vehicle_assignments_vehicle_id ON vehicle_assignments(vehicle_id);
CREATE INDEX idx_vehicle_assignments_courier_id ON vehicle_assignments(courier_id);
CREATE INDEX idx_vehicle_assignments_status ON vehicle_assignments(status);
CREATE INDEX idx_vehicle_assignments_assigned_at ON vehicle_assignments(assigned_at);

CREATE INDEX idx_vehicle_documents_vehicle_id ON vehicle_documents(vehicle_id);
CREATE INDEX idx_vehicle_documents_type ON vehicle_documents(document_type);
CREATE INDEX idx_vehicle_documents_expires_at ON vehicle_documents(expires_at);

CREATE INDEX idx_vehicle_maintenance_vehicle_id ON vehicle_maintenance(vehicle_id);
CREATE INDEX idx_vehicle_maintenance_service_date ON vehicle_maintenance(service_date);