package com.autopulse.fleet_service.repository;

import com.autopulse.fleet_service.model.VehicleDocument;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, UUID> {

    Page<VehicleDocument> findByVehicleId(UUID vehicleId, Pageable pageable);

    Page<VehicleDocument> findByExpiresAtBefore(LocalDate date, Pageable pageable);

    Page<VehicleDocument> findByExpiresAtLessThanEqualAndExpirationNotificationSentFalse(
            LocalDate date,
            Pageable pageable
    );
}