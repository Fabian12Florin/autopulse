package com.autopulse.fleet_service.repository;

import com.autopulse.fleet_service.model.Vehicle;
import com.autopulse.fleet_service.model.VehicleStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    boolean existsByLicensePlate(String licensePlate);
    boolean existsByVin(String vin);
    boolean existsByLicensePlateAndIdNot(String licensePlate, UUID id);
    boolean existsByVinAndIdNot(String vin, UUID id);
    Page<Vehicle> findByDepotIdAndStatus(UUID depotId, VehicleStatus status, Pageable pageable);
}