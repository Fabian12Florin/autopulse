CREATE TABLE vehicle_documents (
                                   id UUID PRIMARY KEY,
                                   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   vehicle_id UUID NOT NULL,
                                   document_type VARCHAR(40) NOT NULL,
                                   issued_at DATE NOT NULL,
                                   expires_at DATE NOT NULL,
                                   cost NUMERIC(12,2) NOT NULL,
                                   description VARCHAR(500),

                                   CONSTRAINT fk_vehicle_documents_vehicle
                                       FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),

                                   CONSTRAINT chk_vehicle_documents_type
                                       CHECK (document_type IN ('INSURANCE', 'VIGNETTE', 'PERIODIC_INSPECTION')),

                                   CONSTRAINT chk_vehicle_documents_cost
                                       CHECK (cost >= 0),

                                   CONSTRAINT chk_vehicle_documents_dates
                                       CHECK (expires_at >= issued_at)
);