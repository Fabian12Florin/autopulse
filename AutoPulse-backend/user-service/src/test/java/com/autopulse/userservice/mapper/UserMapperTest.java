package com.autopulse.userservice.mapper;

import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.testutil.TestDataFactory;
import com.autopulse.userservice.web.dto.UpdateMyUserRequest;
import com.autopulse.userservice.web.dto.UpdateUserRequest;
import com.autopulse.userservice.web.dto.UserResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    @Test
    void createsUserWithNormalizedFieldsAndActiveState() {
        UUID keycloakId = UUID.randomUUID();

        User user = UserMapper.toNewUser(keycloakId, " USER@Example.com ", " Jane ", " Driver ", " +40722123456 ");

        assertThat(user.getKeycloakUserId()).isEqualTo(keycloakId);
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Driver");
        assertThat(user.getPhoneNumber()).isEqualTo("+40722123456");
        assertThat(user.getActive()).isTrue();
    }

    @Test
    void updatesAllUserFieldsFromAdminRequest() {
        User user = new User();

        UserMapper.updateUser(user, new UpdateUserRequest(" NEW@Example.com ", " Ada ", " Lovelace ", " +40722123457 ", false));

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getFirstName()).isEqualTo("Ada");
        assertThat(user.getLastName()).isEqualTo("Lovelace");
        assertThat(user.getPhoneNumber()).isEqualTo("+40722123457");
        assertThat(user.getActive()).isFalse();
    }

    @Test
    void updatesOnlyBaseFieldsForCurrentUser() {
        User user = new User();
        user.setActive(Boolean.TRUE);

        UserMapper.updateCurrentUser(user, new UpdateMyUserRequest(" ME@Example.com ", " Current ", " User ", " +40722123458 "));

        assertThat(user.getEmail()).isEqualTo("me@example.com");
        assertThat(user.getFirstName()).isEqualTo("Current");
        assertThat(user.getLastName()).isEqualTo("User");
        assertThat(user.getPhoneNumber()).isEqualTo("+40722123458");
        assertThat(user.getActive()).isTrue();
    }

    @Test
    void mapsNullsAndResponsesSafely() {
        User user = UserMapper.toNewUser(null, null, null, null, null);
        user.setActive(null);

        assertThat(UserMapper.toResponse(null)).isNull();
        assertThat(UserMapper.toResponse(user).active()).isFalse();
        assertThat(UserMapper.normalizeEmail(null)).isNull();
        assertThat(UserMapper.normalizeText(null)).isNull();
        assertThat(UserMapper.normalizePhone(null)).isNull();
    }

    @Test
    void mapsProfileIdsWhenPresent() {
        User user = TestDataFactory.user();
        user.setDispatcherProfile(TestDataFactory.dispatcherProfile(user));
        user.setCourierProfile(TestDataFactory.courierProfile(user));

        UserResponse response = UserMapper.toResponse(user);

        assertThat(response.dispatcherProfileId()).isEqualTo(TestDataFactory.DISPATCHER_PROFILE_ID);
        assertThat(response.courierProfileId()).isEqualTo(TestDataFactory.COURIER_PROFILE_ID);
    }
}
