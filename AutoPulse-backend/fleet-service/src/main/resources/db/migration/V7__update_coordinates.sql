-- V7__move_depot_coordinates_to_contact_address.sql

ALTER TABLE depots
    RENAME COLUMN latitude TO depot_latitude;

ALTER TABLE depots
    RENAME COLUMN longitude TO depot_longitude;

ALTER TABLE depots
    RENAME CONSTRAINT chk_depots_latitude TO chk_depots_depot_latitude;

ALTER TABLE depots
    RENAME CONSTRAINT chk_depots_longitude TO chk_depots_depot_longitude;

ALTER TABLE depots
    ALTER COLUMN depot_latitude TYPE DOUBLE PRECISION
        USING depot_latitude::DOUBLE PRECISION,
    ALTER COLUMN depot_longitude TYPE DOUBLE PRECISION
        USING depot_longitude::DOUBLE PRECISION;

ALTER TABLE depots
    ALTER COLUMN depot_latitude SET NOT NULL,
    ALTER COLUMN depot_longitude SET NOT NULL;