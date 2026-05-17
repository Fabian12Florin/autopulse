package com.autopulse.userservice.testutil;

import com.autopulse.userservice.config.KeycloakAdminProperties;
import com.autopulse.userservice.model.entity.CourierProfile;
import com.autopulse.userservice.model.entity.DispatcherProfile;
import com.autopulse.userservice.model.entity.User;
import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.web.dto.ChangePasswordRequest;
import com.autopulse.userservice.web.dto.CreateCourierRequest;
import com.autopulse.userservice.web.dto.CreateDispatcherRequest;
import com.autopulse.userservice.web.dto.LoginRequest;
import com.autopulse.userservice.web.dto.LogoutRequest;
import com.autopulse.userservice.web.dto.PasswordResetRequest;
import com.autopulse.userservice.web.dto.RefreshTokenRequest;
import com.autopulse.userservice.web.dto.TokenResponse;
import com.autopulse.userservice.web.dto.UpdateCourierAvailabilityRequest;
import com.autopulse.userservice.web.dto.UpdateCourierRequest;
import com.autopulse.userservice.web.dto.UpdateDispatcherRequest;
import com.autopulse.userservice.web.dto.UpdateMyUserRequest;
import com.autopulse.userservice.web.dto.UpdateUserRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

public final class TestDataFactory {

    public static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID KEYCLOAK_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    public static final UUID DISPATCHER_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000021");
    public static final UUID COURIER_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000031");
    public static final UUID DEPOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000041");

    private TestDataFactory() {
    }

    public static KeycloakAdminProperties keycloakProperties() {
        KeycloakAdminProperties properties = new KeycloakAdminProperties();
        properties.setBaseUrl("http://keycloak.test/");
        properties.setRealm("autopulse");
        properties.setClientId("user-service-client");
        properties.setClientSecret("secret");
        return properties;
    }

    public static User user() {
        return user(USER_ID, KEYCLOAK_ID, "driver@example.com", true);
    }

    public static User user(UUID id, UUID keycloakUserId, String email, boolean active) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setKeycloakUserId(keycloakUserId);
        user.setEmail(email);
        user.setFirstName("Jane");
        user.setLastName("Driver");
        user.setPhoneNumber("+40722123456");
        user.setActive(active);
        return user;
    }

    public static DispatcherProfile dispatcherProfile() {
        return dispatcherProfile(user());
    }

    public static DispatcherProfile dispatcherProfile(User user) {
        DispatcherProfile profile = new DispatcherProfile();
        ReflectionTestUtils.setField(profile, "id", DISPATCHER_PROFILE_ID);
        profile.setUser(user);
        profile.setRegionCode("RO-B");
        return profile;
    }

    public static CourierProfile courierProfile() {
        return courierProfile(AvailabilityStatus.AVAILABLE, "RO-B");
    }

    public static CourierProfile courierProfile(AvailabilityStatus status, String regionCode) {
        return courierProfile(user(), status, regionCode);
    }

    public static CourierProfile courierProfile(User user) {
        return courierProfile(user, AvailabilityStatus.AVAILABLE, "RO-B");
    }

    public static CourierProfile courierProfile(User user, AvailabilityStatus status, String regionCode) {
        CourierProfile profile = new CourierProfile();
        ReflectionTestUtils.setField(profile, "id", COURIER_PROFILE_ID);
        profile.setUser(user);
        profile.setDepotId(DEPOT_ID);
        profile.setRegionCode(regionCode);
        profile.setAvailabilityStatus(status);
        return profile;
    }

    public static CreateDispatcherRequest createDispatcherRequest() {
        return new CreateDispatcherRequest(" DISPATCHER@Example.com ", " Ada ", " Lovelace ", " +40722123456 ", " ro-b ");
    }

    public static UpdateDispatcherRequest updateDispatcherRequest(boolean active) {
        return new UpdateDispatcherRequest(" updated@example.com ", " Grace ", " Hopper ", " +40722123457 ", " ro-c ", active);
    }

    public static CreateCourierRequest createCourierRequest(AvailabilityStatus status, String regionCode) {
        return new CreateCourierRequest(" COURIER@Example.com ", " Linus ", " Torvalds ", " +40722123458 ",
                DEPOT_ID, regionCode, status);
    }

    public static UpdateCourierRequest updateCourierRequest(AvailabilityStatus status, boolean active, String regionCode) {
        return new UpdateCourierRequest(" updated-courier@example.com ", " Alan ", " Turing ", " +40722123459 ",
                DEPOT_ID, regionCode, status, active);
    }

    public static UpdateCourierAvailabilityRequest availabilityRequest(AvailabilityStatus status) {
        return new UpdateCourierAvailabilityRequest(status);
    }

    public static UpdateUserRequest updateUserRequest(boolean active) {
        return new UpdateUserRequest(" updated@example.com ", " Grace ", " Hopper ", " +40722123457 ", active);
    }

    public static UpdateMyUserRequest updateMyUserRequest() {
        return new UpdateMyUserRequest(" me@example.com ", " Current ", " User ", " +40722123460 ");
    }

    public static ChangePasswordRequest changePasswordRequest() {
        return new ChangePasswordRequest("OldPass123", "NewPass123", "NewPass123");
    }

    public static LoginRequest loginRequest() {
        return new LoginRequest(" USER@Example.com ", "Password123");
    }

    public static RefreshTokenRequest refreshTokenRequest() {
        return new RefreshTokenRequest("refresh-token");
    }

    public static LogoutRequest logoutRequest() {
        return new LogoutRequest("refresh-token");
    }

    public static PasswordResetRequest passwordResetRequest() {
        return new PasswordResetRequest(" USER@Example.com ");
    }

    public static TokenResponse tokenResponse(String refreshToken) {
        return new TokenResponse("access", refreshToken, 60, 600, "Bearer", "openid", "session");
    }
}
