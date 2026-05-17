package com.autopulse.geographyservice.service.impl;

import com.autopulse.common.exception.BadRequestException;
import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.geographyservice.mapper.LocalityMapper;
import com.autopulse.geographyservice.mapper.RegionMapper;
import com.autopulse.geographyservice.model.entity.Locality;
import com.autopulse.geographyservice.model.entity.Region;
import com.autopulse.geographyservice.repository.LocalityRepository;
import com.autopulse.geographyservice.repository.RegionRepository;
import com.autopulse.geographyservice.repository.specification.LocalitySpecifications;
import com.autopulse.geographyservice.service.LocalityService;
import com.autopulse.geographyservice.web.dto.CreateLocalityRequest;
import com.autopulse.geographyservice.web.dto.LocalityReferenceResponse;
import com.autopulse.geographyservice.web.dto.LocalityResponse;
import com.autopulse.geographyservice.web.dto.UpdateLocalityRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LocalityServiceImpl implements LocalityService {

    private final LocalityRepository localityRepository;
    private final RegionRepository regionRepository;

    public LocalityServiceImpl(LocalityRepository localityRepository,
                               RegionRepository regionRepository) {
        this.localityRepository = localityRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    @Transactional
    public LocalityResponse create(CreateLocalityRequest request) {
        String normalizedCode = LocalityMapper.normalizeCode(request.code());
        ensureCodeIsUnique(normalizedCode, null);

        Region region = getRegion(request.regionId());
        Locality locality = LocalityMapper.toNewEntity(region, request);
        locality = localityRepository.saveAndFlush(locality);
        return LocalityMapper.toResponse(locality);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalityResponse getById(UUID localityId) {
        return LocalityMapper.toResponse(getLocality(localityId));
    }

    @Override
    @Transactional(readOnly = true)
    public LocalityResponse getByCode(String code) {
        return LocalityMapper.toResponse(getLocalityByCode(code));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LocalityResponse> search(String name,
                                         String code,
                                         UUID regionId,
                                         String regionCode,
                                         Pageable pageable) {
        validateRegionFilters(regionId, regionCode);
        return localityRepository.findAll(LocalitySpecifications.withFilters(name, code, regionId, regionCode), pageable)
                .map(LocalityMapper::toResponse);
    }

    @Override
    @Transactional
    public LocalityResponse update(UUID localityId, UpdateLocalityRequest request) {
        Locality locality = getLocality(localityId);
        String normalizedCode = LocalityMapper.normalizeCode(request.code());
        ensureCodeIsUnique(normalizedCode, localityId);

        Region region = getRegion(request.regionId());
        LocalityMapper.updateEntity(locality, region, request);
        Locality savedLocality = localityRepository.saveAndFlush(locality);
        return LocalityMapper.toResponse(savedLocality);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalityReferenceResponse getReferenceById(UUID localityId) {
        return LocalityMapper.toReferenceResponse(getLocality(localityId));
    }

    @Override
    @Transactional(readOnly = true)
    public LocalityReferenceResponse getReferenceByCode(String code) {
        return LocalityMapper.toReferenceResponse(getLocalityByCode(code));
    }

    private Locality getLocality(UUID localityId) {
        return localityRepository.findById(localityId)
                .orElseThrow(() -> new NotFoundException("Locality was not found"));
    }

    private Locality getLocalityByCode(String code) {
        String normalizedCode = LocalityMapper.normalizeCode(code);
        return localityRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new NotFoundException("Locality was not found"));
    }

    private Region getRegion(UUID regionId) {
        return regionRepository.findById(regionId)
                .orElseThrow(() -> new NotFoundException("Region was not found"));
    }

    private void ensureCodeIsUnique(String code, UUID localityId) {
        boolean exists = localityId == null
                ? localityRepository.existsByCodeIgnoreCase(code)
                : localityRepository.existsByCodeIgnoreCaseAndIdNot(code, localityId);

        if (exists) {
            throw new ConflictException("Locality code already exists");
        }
    }

    private void validateRegionFilters(UUID regionId, String regionCode) {
        if (regionId == null || regionCode == null || regionCode.isBlank()) {
            return;
        }

        Region region = getRegion(regionId);
        String normalizedRegionCode = RegionMapper.normalizeCode(regionCode);
        if (!region.getCode().equalsIgnoreCase(normalizedRegionCode)) {
            throw new BadRequestException("Provided region filters are inconsistent");
        }
    }
}
