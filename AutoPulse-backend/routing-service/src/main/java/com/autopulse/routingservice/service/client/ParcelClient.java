package com.autopulse.routingservice.service.client;

import com.autopulse.routingservice.model.enums.RouteType;
import com.autopulse.routingservice.service.client.dto.ParcelRoutableResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "parcel-service")
public interface ParcelClient {

    @GetMapping("/api/parcels/internal/routable")
    List<ParcelRoutableResponse> getRoutableParcels(
            @RequestParam String depotCode,
            @RequestParam int limit,
            @RequestParam RouteType routeType
            );
}
