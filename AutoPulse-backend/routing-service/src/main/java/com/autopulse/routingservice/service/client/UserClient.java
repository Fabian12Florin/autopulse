package com.autopulse.routingservice.service.client;

import com.autopulse.routingservice.service.client.dto.PageResponse;
import com.autopulse.routingservice.service.client.dto.UserCourierResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/query/couriers/available")
    PageResponse<UserCourierResponse> getAvailableCouriers(
            @RequestParam UUID depotId,
            @RequestParam int page,
            @RequestParam int size
    );
}
