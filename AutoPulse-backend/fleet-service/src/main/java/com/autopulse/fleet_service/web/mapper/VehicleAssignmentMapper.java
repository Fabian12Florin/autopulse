package com.autopulse.fleet_service.web.mapper;

import com.autopulse.fleet_service.model.Vehicle;
import com.autopulse.fleet_service.model.VehicleAssignment;
import com.autopulse.fleet_service.model.VehicleAssignmentStatus;
import com.autopulse.fleet_service.web.dto.vehicle_assignment.CreateVehicleAssignmentRequest;
import com.autopulse.fleet_service.web.dto.vehicle_assignment.VehicleAssignmentResponse;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class VehicleAssignmentMapper {

    public VehicleAssignment toEntity(CreateVehicleAssignmentRequest request, Vehicle vehicle) {
        VehicleAssignment assignment = new VehicleAssignment();
        assignment.setVehicle(vehicle);
        assignment.setCourierId(request.courierId());
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setStatus(VehicleAssignmentStatus.ACTIVE);
        return assignment;
    }

    public VehicleAssignmentResponse toResponse(VehicleAssignment assignment) {
        return new VehicleAssignmentResponse(
                assignment.getId(),
                assignment.getVehicle().getId(),
                assignment.getCourierId(),
                assignment.getAssignedAt(),
                assignment.getUnassignedAt(),
                assignment.getStatus(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }
}