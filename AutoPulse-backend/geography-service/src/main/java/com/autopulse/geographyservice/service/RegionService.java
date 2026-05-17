package com.autopulse.geographyservice.service;

import com.autopulse.geographyservice.web.dto.CreateRegionRequest;
import com.autopulse.geographyservice.web.dto.RegionReferenceResponse;
import com.autopulse.geographyservice.web.dto.RegionResponse;
import com.autopulse.geographyservice.web.dto.UpdateRegionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RegionService {

    RegionResponse create(CreateRegionRequest request);

    RegionResponse getById(UUID regionId);

    RegionResponse getByCode(String code);

    Page<RegionResponse> search(String name,
                                String code,
                                Pageable pageable);

    RegionResponse update(UUID regionId, UpdateRegionRequest request);

    RegionReferenceResponse getReferenceById(UUID regionId);

    RegionReferenceResponse getReferenceByCode(String code);
}
