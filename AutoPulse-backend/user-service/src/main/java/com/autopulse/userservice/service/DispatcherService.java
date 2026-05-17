package com.autopulse.userservice.service;

import com.autopulse.userservice.web.dto.CreateDispatcherRequest;
import com.autopulse.userservice.web.dto.DispatcherResponse;
import com.autopulse.userservice.web.dto.UpdateDispatcherRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DispatcherService {

    DispatcherResponse create(CreateDispatcherRequest request);

    DispatcherResponse getById(UUID dispatcherProfileId);

    Page<DispatcherResponse> search(String regionCode,
                                    Boolean active,
                                    Pageable pageable);

    DispatcherResponse update(UUID dispatcherProfileId, UpdateDispatcherRequest request);
}
