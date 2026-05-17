package com.autopulse.fleet_service.web.controller;

import com.autopulse.fleet_service.service.DepotService;
import com.autopulse.fleet_service.web.dto.depot.CreateDepotRequest;
import com.autopulse.fleet_service.web.dto.depot.DepotCoordinatesResponse;
import com.autopulse.fleet_service.web.dto.depot.DepotResponse;
import com.autopulse.fleet_service.web.dto.depot.UpdateDepotRequest;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fleet/depots")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
public class DepotController {

    private final DepotService depotService;

    public DepotController(DepotService depotService) {
        this.depotService = depotService;
    }

    @PostMapping
    public DepotResponse create(@Valid @RequestBody CreateDepotRequest request) {
        return depotService.create(request);
    }

    @GetMapping
    public Page<DepotResponse> getAll(
            @ParameterObject @PageableDefault() Pageable pageable
    ) {
        return depotService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public DepotResponse getById(@PathVariable UUID id) {
        return depotService.getById(id);
    }

    @PutMapping("/{id}")
    public DepotResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDepotRequest request) {
        return depotService.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    public DepotResponse activate(@PathVariable UUID id) {
        return depotService.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    public DepotResponse deactivate(@PathVariable UUID id) {
        return depotService.deactivate(id);
    }

    @GetMapping("/{id}/coordinates")
    public DepotCoordinatesResponse getDepotCoordinates(@PathVariable UUID id) {
        return depotService.getDepotCoordinates(id);
    }

    @GetMapping("/verify_depot/{depotCode}")
    public Boolean verifyDepotCode(@PathVariable String depotCode) {
        return depotService.verifyDepotCode(depotCode);
    }
}