ALTER TABLE vehicle_documents
    ADD COLUMN expiration_notification_sent BOOLEAN NOT NULL DEFAULT FALSE;