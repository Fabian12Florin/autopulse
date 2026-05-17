package com.autopulse.parcelservice.web.controller;

import com.autopulse.parcelservice.model.enums.RouteType;
import com.autopulse.parcelservice.service.ParcelApplicationService;
import com.autopulse.parcelservice.web.dto.query.ParcelSearchRequest;
import com.autopulse.parcelservice.web.dto.request.CreateParcelRequest;
import com.autopulse.parcelservice.web.dto.request.UpdateParcelRequest;
import com.autopulse.parcelservice.web.dto.request.VerifyDeliveryCodeRequest;
import com.autopulse.parcelservice.web.dto.response.DeliveryCodeVerificationResponse;
import com.autopulse.parcelservice.web.dto.response.ParcelPinResponse;
import com.autopulse.parcelservice.web.dto.response.ParcelResponse;
import com.autopulse.parcelservice.web.dto.response.ParcelSummaryResponse;
import com.autopulse.parcelservice.web.dto.response.RoutableParcelResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parcels")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'COURIER')")
public class ParcelController {

    private final ParcelApplicationService parcelApplicationService;

    @PostMapping
    public ResponseEntity<ParcelResponse> createParcel(@Valid @RequestBody CreateParcelRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parcelApplicationService.createParcel(req));
    }

    @GetMapping("/{parcelId}")
    public ParcelResponse getParcel(@PathVariable UUID parcelId) {
        return parcelApplicationService.getParcel(parcelId);
    }

    @GetMapping("/awb/{awb}")
    public ParcelResponse getParcelByAwb(@PathVariable String awb) {
        return parcelApplicationService.getParcelByAwb(awb);
    }

    @GetMapping("/awb/{awb}/pin")
    public ParcelPinResponse getParcelPinByAwb(@PathVariable String awb) {
        return parcelApplicationService.getParcelPinByAwb(awb);
    }

    @PostMapping("/verify-delivery-code")
    public DeliveryCodeVerificationResponse verifyDeliveryCode(@Valid @RequestBody VerifyDeliveryCodeRequest request) {
        return parcelApplicationService.verifyDeliveryCode(request);
    }

    @GetMapping("/internal/routable")
    public List<RoutableParcelResponse> getRoutableParcels(
            @RequestParam String depotCode,
            @RequestParam RouteType routeType,
            @RequestParam(defaultValue = "500") int limit
    ) {
        return parcelApplicationService.getRoutableParcels(depotCode, routeType, limit);
    }

    @GetMapping
    public Page<ParcelSummaryResponse> searchParcels(@ModelAttribute ParcelSearchRequest criteria) {
        return parcelApplicationService.searchParcels(criteria);
    }

    @PutMapping("/{parcelId}")
    public ParcelResponse updateParcel(@PathVariable UUID parcelId, @Valid @RequestBody UpdateParcelRequest req) {
        return parcelApplicationService.updateParcel(parcelId, req);
    }
}