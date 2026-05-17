package com.autopulse.fleet_service.web.controller;

import com.autopulse.fleet_service.service.VehicleMaintenanceService;
import com.autopulse.fleet_service.web.dto.vehicle_maintenance.CreateVehicleMaintenanceRequest;
import com.autopulse.fleet_service.web.dto.vehicle_maintenance.UpdateVehicleMaintenanceRequest;
import com.autopulse.fleet_service.web.dto.vehicle_maintenance.VehicleMaintenanceResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fleet")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
public class VehicleMaintenanceController {

    private final VehicleMaintenanceService service;

    public VehicleMaintenanceController(VehicleMaintenanceService service) {
        this.service = service;
    }

    @PostMapping("/vehicles/{vehicleId}/maintenance-records")
    public VehicleMaintenanceResponse create(@PathVariable UUID vehicleId,
                                             @Valid @RequestBody CreateVehicleMaintenanceRequest request) {
        return service.create(vehicleId, request);
    }

    @GetMapping("/vehicles/{vehicleId}/maintenance-records")
    public Page<VehicleMaintenanceResponse> getByVehicle(
            @PathVariable UUID vehicleId,
            @ParameterObject @PageableDefault() Pageable pageable
    ) {
        return service.getByVehicleId(vehicleId, pageable);
    }

    @GetMapping("/maintenance-records/{recordId}")
    public VehicleMaintenanceResponse getById(@PathVariable UUID recordId) {
        return service.getById(recordId);
    }

    @PutMapping("/maintenance-records/{recordId}")
    public VehicleMaintenanceResponse update(
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateVehicleMaintenanceRequest request) {
        return service.update(recordId, request);
    }

    @DeleteMapping("/maintenance-records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID recordId) {
        service.delete(recordId);
    }
}