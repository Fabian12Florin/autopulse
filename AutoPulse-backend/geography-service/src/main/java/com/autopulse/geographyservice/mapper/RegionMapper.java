package com.autopulse.geographyservice.mapper;

import com.autopulse.geographyservice.model.entity.Region;
import com.autopulse.geographyservice.web.dto.CreateRegionRequest;
import com.autopulse.geographyservice.web.dto.RegionReferenceResponse;
import com.autopulse.geographyservice.web.dto.RegionResponse;
import com.autopulse.geographyservice.web.dto.UpdateRegionRequest;

import java.util.Locale;

public final class RegionMapper {

    private RegionMapper() {
    }

    public static Region toNewEntity(CreateRegionRequest request) {
        Region region = new Region();
        region.setName(normalizeText(request.name()));
        region.setCode(normalizeCode(request.code()));
        return region;
    }

    public static void updateEntity(Region region, UpdateRegionRequest request) {
        region.setName(normalizeText(request.name()));
        region.setCode(normalizeCode(request.code()));
    }

    public static RegionResponse toResponse(Region region) {
        if (region == null) {
            return null;
        }

        return new RegionResponse(
                region.getId(),
                region.getName(),
                region.getCode(),
                region.getCreatedAt(),
                region.getUpdatedAt()
        );
    }

    public static RegionReferenceResponse toReferenceResponse(Region region) {
        if (region == null) {
            return null;
        }

        return new RegionReferenceResponse(
                region.getId(),
                region.getName(),
                region.getCode()
        );
    }

    public static String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    public static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
