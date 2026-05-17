package com.autopulse.fleet_service.repository;

import com.autopulse.fleet_service.model.VehicleAssignment;
import com.autopulse.fleet_service.model.VehicleAssignmentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleAssignmentRepository extends JpaRepository<VehicleAssignment, UUID> {
    Page<VehicleAssignment> findByCourierId(UUID courierId, Pageable pageable);
    Optional<VehicleAssignment> findFirstByVehicleIdAndStatus(UUID vehicleId, VehicleAssignmentStatus status);
    Optional<VehicleAssignment> findFirstByCourierIdAndStatus(UUID courierId, VehicleAssignmentStatus status);
}