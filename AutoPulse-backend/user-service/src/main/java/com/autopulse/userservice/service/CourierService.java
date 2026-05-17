package com.autopulse.userservice.service;

import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.web.dto.CourierResponse;
import com.autopulse.userservice.web.dto.CreateCourierRequest;
import com.autopulse.userservice.web.dto.UpdateCourierAvailabilityRequest;
import com.autopulse.userservice.web.dto.UpdateCourierRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CourierService {

    CourierResponse create(CreateCourierRequest request);

    CourierResponse getById(UUID courierProfileId);

    Page<CourierResponse> search(UUID depotId,
                                 String regionCode,
                                 AvailabilityStatus availabilityStatus,
                                 Boolean active,
                                 Pageable pageable);

    Page<CourierResponse> searchAvailableByDepot(UUID depotId, Pageable pageable);

    CourierResponse update(UUID courierProfileId, UpdateCourierRequest request);

    CourierResponse updateAvailability(UUID courierProfileId, UpdateCourierAvailabilityRequest request);

    CourierResponse updateMyAvailability(UpdateCourierAvailabilityRequest request);
}
