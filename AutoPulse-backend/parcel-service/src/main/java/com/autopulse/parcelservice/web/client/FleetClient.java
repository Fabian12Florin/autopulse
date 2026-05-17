package com.autopulse.parcelservice.web.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "fleet-service")
public interface FleetClient {

    @GetMapping("/api/fleet/depots/verify_depot/{depotCode}")
    Boolean verifyDepotCode(@PathVariable String depotCode);
}
