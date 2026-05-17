package com.autopulse.userservice.mapper;

import com.autopulse.userservice.model.entity.DispatcherProfile;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.web.dto.CreateDispatcherRequest;
import com.autopulse.userservice.web.dto.DispatcherResponse;
import com.autopulse.userservice.web.dto.UpdateDispatcherRequest;

import java.util.Locale;

public final class DispatcherProfileMapper {

    private DispatcherProfileMapper() {
    }

    public static DispatcherProfile toNewEntity(User user, CreateDispatcherRequest request) {
        DispatcherProfile profile = new DispatcherProfile();
        profile.setUser(user);
        profile.setRegionCode(normalizeRegionCode(request.regionCode()));
        return profile;
    }

    public static void updateEntity(DispatcherProfile profile, UpdateDispatcherRequest request) {
        profile.setRegionCode(normalizeRegionCode(request.regionCode()));
    }

    public static DispatcherResponse toResponse(DispatcherProfile profile) {
        if (profile == null) {
            return null;
        }

        return new DispatcherResponse(
                profile.getId(),
                profile.getRegionCode(),
                UserMapper.toResponse(profile.getUser()),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    public static String normalizeRegionCode(String regionCode) {
        return regionCode == null ? null : regionCode.trim().toUpperCase(Locale.ROOT);
    }
}
