package com.autopulse.fleet_service.web.controller;

import com.autopulse.fleet_service.service.VehicleDocumentService;
import com.autopulse.fleet_service.web.dto.vehicle_document.CreateVehicleDocumentRequest;
import com.autopulse.fleet_service.web.dto.vehicle_document.UpdateVehicleDocumentRequest;
import com.autopulse.fleet_service.web.dto.vehicle_document.VehicleDocumentResponse;
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
public class VehicleDocumentController {

    private final VehicleDocumentService service;

    public VehicleDocumentController(VehicleDocumentService service) {
        this.service = service;
    }

    @PostMapping("/vehicles/{vehicleId}/documents")
    public VehicleDocumentResponse create(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody CreateVehicleDocumentRequest request
    ) {
        return service.create(vehicleId, request);
    }

    @GetMapping("/vehicles/{vehicleId}/documents")
    public Page<VehicleDocumentResponse> getByVehicle(
            @PathVariable UUID vehicleId,
            @ParameterObject @PageableDefault() Pageable pageable
    ) {
        return service.getByVehicleId(vehicleId, pageable);
    }

    @GetMapping("/documents/{documentId}")
    public VehicleDocumentResponse getById(@PathVariable UUID documentId) {
        return service.getById(documentId);
    }

    @PutMapping("/documents/{documentId}")
    public VehicleDocumentResponse update(
            @PathVariable UUID documentId,
            @Valid @RequestBody UpdateVehicleDocumentRequest request
    ) {
        return service.update(documentId, request);
    }

    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID documentId) {
        service.delete(documentId);
    }

    @GetMapping("/documents/expired")
    public Page<VehicleDocumentResponse> getExpired(
            @ParameterObject @PageableDefault() Pageable pageable
    ) {
        return service.getExpired(pageable);
    }
}