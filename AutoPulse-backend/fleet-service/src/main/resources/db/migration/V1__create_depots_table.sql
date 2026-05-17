CREATE TABLE depots (
                        id UUID PRIMARY KEY,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        name VARCHAR(150) NOT NULL,
                        contact_name VARCHAR(120) NOT NULL,
                        contact_email VARCHAR(180) NOT NULL,
                        contact_phone VARCHAR(40) NOT NULL,
                        address_street VARCHAR(180) NOT NULL,
                        address_number VARCHAR(40) NOT NULL,
                        locality_id UUID NOT NULL,
                        locality_code VARCHAR(50),
                        depot_code VARCHAR(50) NOT NULL,
                        latitude NUMERIC(10, 8),
                        longitude NUMERIC(11, 8),
                        active BOOLEAN NOT NULL DEFAULT TRUE,

                        CONSTRAINT uk_depots_depot_code UNIQUE (depot_code),
                        CONSTRAINT chk_depots_latitude
                            CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
                        CONSTRAINT chk_depots_longitude
                            CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180))
);