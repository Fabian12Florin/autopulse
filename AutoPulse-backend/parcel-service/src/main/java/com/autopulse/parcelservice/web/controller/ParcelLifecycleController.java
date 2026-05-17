package com.autopulse.parcelservice.web.controller;

import com.autopulse.parcelservice.service.ParcelLifecycleService;
import com.autopulse.parcelservice.web.dto.request.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/parcels/{parcelId}")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'COURIER')")
public class ParcelLifecycleController {

    private final ParcelLifecycleService lifecycleService;

    @PatchMapping("/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID parcelId,
            @Valid @RequestBody ParcelStatusUpdateRequest request
    ) {
        lifecycleService.updateStatus(parcelId, request.status());
        return ResponseEntity.noContent().build();
    }
}