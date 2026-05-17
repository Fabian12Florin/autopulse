package com.autopulse.geographyservice.service.impl;

import com.autopulse.common.exception.ConflictException;
import com.autopulse.common.exception.NotFoundException;
import com.autopulse.geographyservice.mapper.RegionMapper;
import com.autopulse.geographyservice.model.entity.Region;
import com.autopulse.geographyservice.repository.RegionRepository;
import com.autopulse.geographyservice.repository.specification.RegionSpecifications;
import com.autopulse.geographyservice.service.RegionService;
import com.autopulse.geographyservice.web.dto.CreateRegionRequest;
import com.autopulse.geographyservice.web.dto.RegionReferenceResponse;
import com.autopulse.geographyservice.web.dto.RegionResponse;
import com.autopulse.geographyservice.web.dto.UpdateRegionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegionServiceImpl implements RegionService {

    private final RegionRepository regionRepository;

    public RegionServiceImpl(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @Override
    @Transactional
    public RegionResponse create(CreateRegionRequest request) {
        String normalizedCode = RegionMapper.normalizeCode(request.code());
        ensureCodeIsUnique(normalizedCode, null);

        Region region = RegionMapper.toNewEntity(request);
        region = regionRepository.saveAndFlush(region);
        return RegionMapper.toResponse(region);
    }

    @Override
    @Transactional(readOnly = true)
    public RegionResponse getById(UUID regionId) {
        return RegionMapper.toResponse(getRegion(regionId));
    }

    @Override
    @Transactional(readOnly = true)
    public RegionResponse getByCode(String code) {
        return RegionMapper.toResponse(getRegionByCode(code));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegionResponse> search(String name,
                                       String code,
                                       Pageable pageable) {
        return regionRepository.findAll(RegionSpecifications.withFilters(name, code), pageable)
                .map(RegionMapper::toResponse);
    }

    @Override
    @Transactional
    public RegionResponse update(UUID regionId, UpdateRegionRequest request) {
        Region region = getRegion(regionId);
        String normalizedCode = RegionMapper.normalizeCode(request.code());
        ensureCodeIsUnique(normalizedCode, regionId);

        RegionMapper.updateEntity(region, request);
        Region savedRegion = regionRepository.saveAndFlush(region);
        return RegionMapper.toResponse(savedRegion);
    }

    @Override
    @Transactional(readOnly = true)
    public RegionReferenceResponse getReferenceById(UUID regionId) {
        return RegionMapper.toReferenceResponse(getRegion(regionId));
    }

    @Override
    @Transactional(readOnly = true)
    public RegionReferenceResponse getReferenceByCode(String code) {
        return RegionMapper.toReferenceResponse(getRegionByCode(code));
    }

    private Region getRegion(UUID regionId) {
        return regionRepository.findById(regionId)
                .orElseThrow(() -> new NotFoundException("Region was not found"));
    }

    private Region getRegionByCode(String code) {
        String normalizedCode = RegionMapper.normalizeCode(code);
        return regionRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new NotFoundException("Region was not found"));
    }

    private void ensureCodeIsUnique(String code, UUID regionId) {
        boolean exists = regionId == null
                ? regionRepository.existsByCodeIgnoreCase(code)
                : regionRepository.existsByCodeIgnoreCaseAndIdNot(code, regionId);

        if (exists) {
            throw new ConflictException("Region code already exists");
        }
    }
}
