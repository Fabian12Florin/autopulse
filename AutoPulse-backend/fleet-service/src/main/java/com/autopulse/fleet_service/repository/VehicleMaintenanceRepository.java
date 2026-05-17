package com.autopulse.fleet_service.repository;

import com.autopulse.fleet_service.model.VehicleMaintenance;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleMaintenanceRepository extends JpaRepository<VehicleMaintenance, UUID> {
    Page<VehicleMaintenance> findByVehicleIdOrderByServiceDateDesc(UUID vehicleId, Pageable pageable);
}