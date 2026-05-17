package com.autopulse.fleet_service.web.controller;

import com.autopulse.fleet_service.service.VehicleAssignmentService;
import com.autopulse.fleet_service.web.dto.vehicle_assignment.CreateVehicleAssignmentRequest;
import com.autopulse.fleet_service.web.dto.vehicle_assignment.VehicleAssignmentResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/fleet")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
public class VehicleAssignmentController {

    private final VehicleAssignmentService service;

    public VehicleAssignmentController(VehicleAssignmentService service) {
        this.service = service;
    }

    @PostMapping("/vehicle-assignments")
    public VehicleAssignmentResponse assign(@Valid @RequestBody CreateVehicleAssignmentRequest request) {
        return service.assign(request);
    }

    @PatchMapping("/vehicle-assignments/{id}/end")
    public VehicleAssignmentResponse endAssignment(@PathVariable UUID id) {
        return service.endAssignment(id);
    }

    @GetMapping("/vehicle-assignments")
    public Page<VehicleAssignmentResponse> getAll(
            @ParameterObject @PageableDefault() Pageable pageable
    ) {
        return service.getAll(pageable);
    }

    @GetMapping("/vehicle-assignments/{id}")
    public VehicleAssignmentResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping("/couriers/{courierId}/vehicle-assignment")
    public VehicleAssignmentResponse getActiveByCourier(@PathVariable UUID courierId) {
        return service.getActiveByCourierId(courierId);
    }

    @GetMapping("/couriers/{courierId}/vehicle-assignments")
    public Page<VehicleAssignmentResponse> getAssignmentsByCourier(
            @PathVariable UUID courierId,
            @ParameterObject @PageableDefault() Pageable pageable
    ) {
        return service.getByCourierId(courierId, pageable);
    }
}