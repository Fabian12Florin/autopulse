CREATE TABLE vehicle_maintenance (
                                     id UUID PRIMARY KEY,
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     vehicle_id UUID NOT NULL,
                                     mileage INTEGER NOT NULL,
                                     service_date DATE NOT NULL,
                                     cost NUMERIC(12,2) NOT NULL,
                                     description VARCHAR(500),
                                     service_provider VARCHAR(150),

                                     CONSTRAINT fk_vehicle_maintenance_vehicle
                                         FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),

                                     CONSTRAINT chk_vehicle_maintenance_mileage
                                         CHECK (mileage >= 0),

                                     CONSTRAINT chk_vehicle_maintenance_cost
                                         CHECK (cost >= 0)
);