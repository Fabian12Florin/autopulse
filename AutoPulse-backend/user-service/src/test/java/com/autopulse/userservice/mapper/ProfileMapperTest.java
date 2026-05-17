package com.autopulse.userservice.mapper;

import com.autopulse.userservice.model.entity.CourierProfile;
import com.autopulse.userservice.model.entity.DispatcherProfile;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileMapperTest {

    @Test
    void mapsCourierCreateUpdateAvailabilityAndResponse() {
        User user = TestDataFactory.user();
        CourierProfile profile = CourierProfileMapper.toNewEntity(
                user,
                TestDataFactory.createCourierRequest(AvailabilityStatus.AVAILABLE, " ro-b ")
        );

        assertThat(profile.getUser()).isSameAs(user);
        assertThat(profile.getDepotId()).isEqualTo(TestDataFactory.DEPOT_ID);
        assertThat(profile.getRegionCode()).isEqualTo("RO-B");
        assertThat(profile.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);

        CourierProfileMapper.updateEntity(profile, TestDataFactory.updateCourierRequest(AvailabilityStatus.OFF_DUTY, true, " ro-c "));
        assertThat(profile.getRegionCode()).isEqualTo("RO-C");
        assertThat(profile.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFF_DUTY);

        CourierProfileMapper.updateAvailability(profile, TestDataFactory.availabilityRequest(AvailabilityStatus.AVAILABLE));
        assertThat(CourierProfileMapper.toResponse(profile).user().email()).isEqualTo(user.getEmail());
        assertThat(CourierProfileMapper.toResponse(null)).isNull();
        assertThat(CourierProfileMapper.normalizeRegionCode(null)).isNull();
    }

    @Test
    void mapsDispatcherCreateUpdateAndResponse() {
        User user = TestDataFactory.user();
        DispatcherProfile profile = DispatcherProfileMapper.toNewEntity(user, TestDataFactory.createDispatcherRequest());

        assertThat(profile.getUser()).isSameAs(user);
        assertThat(profile.getRegionCode()).isEqualTo("RO-B");

        DispatcherProfileMapper.updateEntity(profile, TestDataFactory.updateDispatcherRequest(false));

        assertThat(profile.getRegionCode()).isEqualTo("RO-C");
        assertThat(DispatcherProfileMapper.toResponse(profile).user().email()).isEqualTo(user.getEmail());
        assertThat(DispatcherProfileMapper.toResponse(null)).isNull();
        assertThat(DispatcherProfileMapper.normalizeRegionCode(null)).isNull();
    }
}
