package com.autopulse.userservice.mapper;

import com.autopulse.userservice.model.entity.CourierProfile;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.web.dto.CourierResponse;
import com.autopulse.userservice.web.dto.CreateCourierRequest;
import com.autopulse.userservice.web.dto.UpdateCourierAvailabilityRequest;
import com.autopulse.userservice.web.dto.UpdateCourierRequest;

import java.util.Locale;

public final class CourierProfileMapper {

    private CourierProfileMapper() {
    }

    public static CourierProfile toNewEntity(User user, CreateCourierRequest request) {
        CourierProfile profile = new CourierProfile();
        profile.setUser(user);
        profile.setDepotId(request.depotId());
        profile.setRegionCode(normalizeRegionCode(request.regionCode()));
        profile.setAvailabilityStatus(request.availabilityStatus());
        return profile;
    }

    public static void updateEntity(CourierProfile profile, UpdateCourierRequest request) {
        profile.setDepotId(request.depotId());
        profile.setRegionCode(normalizeRegionCode(request.regionCode()));
        profile.setAvailabilityStatus(request.availabilityStatus());
    }

    public static void updateAvailability(CourierProfile profile, UpdateCourierAvailabilityRequest request) {
        profile.setAvailabilityStatus(request.availabilityStatus());
    }

    public static CourierResponse toResponse(CourierProfile profile) {
        if (profile == null) {
            return null;
        }

        return new CourierResponse(
                profile.getId(),
                profile.getDepotId(),
                profile.getRegionCode(),
                profile.getAvailabilityStatus(),
                UserMapper.toResponse(profile.getUser()),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    public static String normalizeRegionCode(String regionCode) {
        return regionCode == null ? null : regionCode.trim().toUpperCase(Locale.ROOT);
    }
}
