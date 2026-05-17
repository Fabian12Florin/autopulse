package com.autopulse.geographyservice.service;

import com.autopulse.geographyservice.web.dto.CreateLocalityRequest;
import com.autopulse.geographyservice.web.dto.LocalityReferenceResponse;
import com.autopulse.geographyservice.web.dto.LocalityResponse;
import com.autopulse.geographyservice.web.dto.UpdateLocalityRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LocalityService {

    LocalityResponse create(CreateLocalityRequest request);

    LocalityResponse getById(UUID localityId);

    LocalityResponse getByCode(String code);

    Page<LocalityResponse> search(String name,
                                  String code,
                                  UUID regionId,
                                  String regionCode,
                                  Pageable pageable);

    LocalityResponse update(UUID localityId, UpdateLocalityRequest request);

    LocalityReferenceResponse getReferenceById(UUID localityId);

    LocalityReferenceResponse getReferenceByCode(String code);
}
