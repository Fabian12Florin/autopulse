CREATE TABLE vehicles (
                          id UUID PRIMARY KEY,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          depot_id UUID NOT NULL,
                          license_plate VARCHAR(20) NOT NULL,
                          brand VARCHAR(80) NOT NULL,
                          model VARCHAR(80) NOT NULL,
                          vin VARCHAR(50) NOT NULL,
                          year INTEGER NOT NULL,
                          category VARCHAR(30) NOT NULL,
                          fuel_type VARCHAR(30) NOT NULL,
                          capacity_weight DOUBLE PRECISION NOT NULL,
                          capacity_volume DOUBLE PRECISION NOT NULL,
                          status VARCHAR(30) NOT NULL,

                          CONSTRAINT fk_vehicles_depot
                              FOREIGN KEY (depot_id) REFERENCES depots(id),

                          CONSTRAINT uk_vehicles_license_plate UNIQUE (license_plate),
                          CONSTRAINT uk_vehicles_vin UNIQUE (vin),

                          CONSTRAINT chk_vehicles_category
                              CHECK (category IN ('M1', 'M2', 'M3')),

                          CONSTRAINT chk_vehicles_fuel_type
                              CHECK (fuel_type IN ('PETROL', 'DIESEL', 'ELECTRIC', 'HYBRID', 'LPG')),

                          CONSTRAINT chk_vehicles_status
                              CHECK (status IN ('AVAILABLE', 'IN_USE', 'MAINTENANCE', 'OUT_OF_SERVICE')),

                          CONSTRAINT chk_vehicles_year
                              CHECK (year >= 1900 AND year <= 2100),

                          CONSTRAINT chk_vehicles_capacity_weight
                              CHECK (capacity_weight > 0),

                          CONSTRAINT chk_vehicles_capacity_volume
                              CHECK (capacity_volume > 0)
);