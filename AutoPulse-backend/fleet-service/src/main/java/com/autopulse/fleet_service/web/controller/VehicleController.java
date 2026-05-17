package com.autopulse.fleet_service.web.controller;

import com.autopulse.fleet_service.service.VehicleService;
import com.autopulse.fleet_service.web.dto.vehicle.AvailableVehicleSummaryResponse;
import com.autopulse.fleet_service.web.dto.vehicle.CreateVehicleRequest;
import com.autopulse.fleet_service.web.dto.vehicle.UpdateVehicleRequest;
import com.autopulse.fleet_service.web.dto.vehicle.UpdateVehicleStatusRequest;
import com.autopulse.fleet_service.web.dto.vehicle.VehicleResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fleet/vehicles")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public VehicleResponse create(@Valid @RequestBody CreateVehicleRequest request) {
        return vehicleService.create(request);
    }

    @GetMapping
    public Page<VehicleResponse> getAll(
            @ParameterObject @PageableDefault() Pageable pageable
    ) {
        return vehicleService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public VehicleResponse getById(@PathVariable UUID id) {
        return vehicleService.getById(id);
    }

    @PutMapping("/{id}")
    public VehicleResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateVehicleRequest request) {
        return vehicleService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public VehicleResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVehicleStatusRequest request
    ) {
        return vehicleService.updateStatus(id, request);
    }

    @PatchMapping("/{id}/activate")
    public VehicleResponse activate(@PathVariable UUID id) {
        return vehicleService.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    public VehicleResponse deactivate(@PathVariable UUID id) {
        return vehicleService.deactivate(id);
    }

    @GetMapping("/available")
    public Page<VehicleResponse> getAvailableByDepot(
            @RequestParam UUID depotId,
            @ParameterObject @PageableDefault() Pageable pageable
    ) {
        return vehicleService.getAvailableByDepot(depotId, pageable);
    }

    @GetMapping("/depots/{depotId}/available-vehicles")
    public Page<AvailableVehicleSummaryResponse> getAvailableVehiclesByDepot(
            @PathVariable UUID depotId,
            @ParameterObject @PageableDefault() Pageable pageable
    ) {
        return vehicleService.getAvailableSummaryByDepot(depotId, pageable);
    }
}