package com.autopulse.routingservice.service.client;

import com.autopulse.routingservice.service.client.dto.FleetAvailableVehicleResponse;
import com.autopulse.routingservice.service.client.dto.FleetDepotResponse;
import com.autopulse.routingservice.service.client.dto.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "fleet-service")
public interface FleetClient {

    @GetMapping("/api/fleet/depots/{depotId}")
    FleetDepotResponse getDepot(@PathVariable UUID depotId);

    @GetMapping("/api/fleet/vehicles/depots/{depotId}/available-vehicles")
    PageResponse<FleetAvailableVehicleResponse> getAvailableVehicles(
            @PathVariable UUID depotId,
            @RequestParam int page,
            @RequestParam int size
    );
}
