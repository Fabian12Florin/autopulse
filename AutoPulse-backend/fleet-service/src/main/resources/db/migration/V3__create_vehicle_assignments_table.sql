CREATE TABLE vehicle_assignments (
                                     id UUID PRIMARY KEY,
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     vehicle_id UUID NOT NULL,
                                     courier_id UUID NOT NULL,
                                     assigned_at TIMESTAMP NOT NULL,
                                     unassigned_at TIMESTAMP,
                                     status VARCHAR(30) NOT NULL,

                                     CONSTRAINT fk_vehicle_assignments_vehicle
                                         FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),

                                     CONSTRAINT chk_vehicle_assignments_status
                                         CHECK (status IN ('ACTIVE', 'ENDED', 'CANCELLED')),

                                     CONSTRAINT chk_vehicle_assignments_dates
                                         CHECK (unassigned_at IS NULL OR unassigned_at >= assigned_at)
);