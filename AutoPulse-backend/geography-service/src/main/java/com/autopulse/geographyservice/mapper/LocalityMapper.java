package com.autopulse.geographyservice.mapper;

import com.autopulse.geographyservice.model.entity.Locality;
import com.autopulse.geographyservice.model.entity.Region;
import com.autopulse.geographyservice.web.dto.CreateLocalityRequest;
import com.autopulse.geographyservice.web.dto.LocalityReferenceResponse;
import com.autopulse.geographyservice.web.dto.LocalityResponse;
import com.autopulse.geographyservice.web.dto.UpdateLocalityRequest;

import java.util.Locale;

public final class LocalityMapper {

    private LocalityMapper() {
    }

    public static Locality toNewEntity(Region region, CreateLocalityRequest request) {
        Locality locality = new Locality();
        locality.setName(normalizeText(request.name()));
        locality.setCode(normalizeCode(request.code()));
        locality.setRegion(region);
        return locality;
    }

    public static void updateEntity(Locality locality, Region region, UpdateLocalityRequest request) {
        locality.setName(normalizeText(request.name()));
        locality.setCode(normalizeCode(request.code()));
        locality.setRegion(region);
    }

    public static LocalityResponse toResponse(Locality locality) {
        if (locality == null) {
            return null;
        }

        return new LocalityResponse(
                locality.getId(),
                locality.getName(),
                locality.getCode(),
                RegionMapper.toReferenceResponse(locality.getRegion()),
                locality.getCreatedAt(),
                locality.getUpdatedAt()
        );
    }

    public static LocalityReferenceResponse toReferenceResponse(Locality locality) {
        if (locality == null) {
            return null;
        }

        return new LocalityReferenceResponse(
                locality.getId(),
                locality.getName(),
                locality.getCode(),
                RegionMapper.toReferenceResponse(locality.getRegion())
        );
    }

    public static String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    public static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
