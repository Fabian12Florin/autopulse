package com.autopulse.userservice.web.controller;

import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.service.CourierService;
import com.autopulse.userservice.service.UserService;
import com.autopulse.userservice.testutil.TestDataFactory;
import com.autopulse.userservice.web.dto.CourierResponse;
import com.autopulse.userservice.web.dto.PasswordChangeResponse;
import com.autopulse.userservice.web.dto.UserResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourierControllerTest {

    private final UserService userService = mock(UserService.class);
    private final CourierService courierService = mock(CourierService.class);
    private final CourierController controller = new CourierController(userService, courierService);

    @Test
    void delegatesSelfServiceEndpoints() {
        UserResponse user = new UserResponse(TestDataFactory.USER_ID, TestDataFactory.KEYCLOAK_ID,
                TestDataFactory.DISPATCHER_PROFILE_ID, TestDataFactory.COURIER_PROFILE_ID, "user@example.com",
                "Jane", "Driver", "+40722123456", true, null, null);
        CourierResponse courier = new CourierResponse(TestDataFactory.COURIER_PROFILE_ID, TestDataFactory.DEPOT_ID, "RO-B",
                AvailabilityStatus.AVAILABLE, user, null, null);
        PasswordChangeResponse password = new PasswordChangeResponse(TestDataFactory.USER_ID, "user@example.com", "changed");

        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.updateCurrentUser(TestDataFactory.updateMyUserRequest())).thenReturn(user);
        when(userService.changeMyPassword(TestDataFactory.changePasswordRequest())).thenReturn(password);
        when(courierService.updateMyAvailability(TestDataFactory.availabilityRequest(AvailabilityStatus.AVAILABLE))).thenReturn(courier);

        assertThat(controller.getMe().getBody()).isSameAs(user);
        assertThat(controller.updateMe(TestDataFactory.updateMyUserRequest()).getBody()).isSameAs(user);
        assertThat(controller.changeMyPassword(TestDataFactory.changePasswordRequest()).getBody()).isSameAs(password);
        assertThat(controller.updateMyCourierAvailability(TestDataFactory.availabilityRequest(AvailabilityStatus.AVAILABLE)).getBody()).isSameAs(courier);
    }
}
