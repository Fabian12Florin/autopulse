package com.autopulse.routingservice.service.client;

import com.autopulse.routingservice.web.dto.delivery.CreateDeliveryRunRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "delivery-execution-service")
public interface DeliveryExecutionClient {

    @PostMapping("/api/delivery-execution/runs")
    void createDeliveryRun(@RequestBody CreateDeliveryRunRequest request);
}
