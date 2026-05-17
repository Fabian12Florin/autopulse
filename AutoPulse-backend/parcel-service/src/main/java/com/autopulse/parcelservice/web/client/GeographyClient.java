package com.autopulse.parcelservice.web.client;

import com.autopulse.parcelservice.web.client.dto.CoordinatesResponse;
import com.autopulse.parcelservice.web.client.dto.GeocodeAddressRequest;
import com.autopulse.parcelservice.web.client.dto.LocalityReferenceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "geography-service")
public interface GeographyClient {

    @GetMapping("/api/geography/internal/localities/{localityId}")
    LocalityReferenceResponse getLocality(@PathVariable UUID localityId);

    @PostMapping("/api/geography/internal/coordinates")
    CoordinatesResponse resolveCoordinates(@RequestBody GeocodeAddressRequest request);
}