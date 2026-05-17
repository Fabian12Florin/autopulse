package com.autopulse.userservice.web.controller;

import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.service.CourierService;
import com.autopulse.userservice.service.DispatcherService;
import com.autopulse.userservice.service.UserService;
import com.autopulse.userservice.testutil.TestDataFactory;
import com.autopulse.userservice.web.dto.CourierResponse;
import com.autopulse.userservice.web.dto.DispatcherResponse;
import com.autopulse.userservice.web.dto.PasswordResetResponse;
import com.autopulse.userservice.web.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminUserControllerTest {

    private final UserService userService = mock(UserService.class);
    private final DispatcherService dispatcherService = mock(DispatcherService.class);
    private final CourierService courierService = mock(CourierService.class);
    private final AdminUserController controller = new AdminUserController(userService, dispatcherService, courierService);

    @Test
    void delegatesAdminUserAndProfileEndpoints() {
        UserResponse user = userResponse();
        DispatcherResponse dispatcher = dispatcherResponse();
        CourierResponse courier = courierResponse();
        PasswordResetResponse reset = new PasswordResetResponse(TestDataFactory.USER_ID, "user@example.com", "queued");

        when(userService.getById(TestDataFactory.USER_ID)).thenReturn(user);
        when(userService.update(TestDataFactory.USER_ID, TestDataFactory.updateUserRequest(true))).thenReturn(user);
        when(dispatcherService.create(TestDataFactory.createDispatcherRequest())).thenReturn(dispatcher);
        when(dispatcherService.update(TestDataFactory.DISPATCHER_PROFILE_ID, TestDataFactory.updateDispatcherRequest(true))).thenReturn(dispatcher);
        when(courierService.create(TestDataFactory.createCourierRequest(AvailabilityStatus.AVAILABLE, "RO-B"))).thenReturn(courier);
        when(courierService.update(TestDataFactory.COURIER_PROFILE_ID, TestDataFactory.updateCourierRequest(AvailabilityStatus.AVAILABLE, true, "RO-B"))).thenReturn(courier);
        when(userService.activate(TestDataFactory.USER_ID)).thenReturn(user);
        when(userService.deactivate(TestDataFactory.USER_ID)).thenReturn(user);
        when(userService.resetPassword(TestDataFactory.USER_ID)).thenReturn(reset);

        assertThat(controller.getUser(TestDataFactory.USER_ID).getBody()).isSameAs(user);
        assertThat(controller.updateUser(TestDataFactory.USER_ID, TestDataFactory.updateUserRequest(true)).getBody()).isSameAs(user);
        assertThat(controller.createDispatcher(TestDataFactory.createDispatcherRequest()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateDispatcher(TestDataFactory.DISPATCHER_PROFILE_ID, TestDataFactory.updateDispatcherRequest(true)).getBody()).isSameAs(dispatcher);
        assertThat(controller.createCourier(TestDataFactory.createCourierRequest(AvailabilityStatus.AVAILABLE, "RO-B")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateCourier(TestDataFactory.COURIER_PROFILE_ID, TestDataFactory.updateCourierRequest(AvailabilityStatus.AVAILABLE, true, "RO-B")).getBody()).isSameAs(courier);
        assertThat(controller.activateUser(TestDataFactory.USER_ID).getBody()).isSameAs(user);
        assertThat(controller.deactivateUser(TestDataFactory.USER_ID).getBody()).isSameAs(user);
        assertThat(controller.resetPassword(TestDataFactory.USER_ID).getBody()).isSameAs(reset);
    }

    private UserResponse userResponse() {
        return new UserResponse(TestDataFactory.USER_ID, TestDataFactory.KEYCLOAK_ID,
                TestDataFactory.DISPATCHER_PROFILE_ID, TestDataFactory.COURIER_PROFILE_ID, "user@example.com",
                "Jane", "Driver", "+40722123456", true, null, null);
    }

    private DispatcherResponse dispatcherResponse() {
        return new DispatcherResponse(TestDataFactory.DISPATCHER_PROFILE_ID, "RO-B", userResponse(), null, null);
    }

    private CourierResponse courierResponse() {
        return new CourierResponse(TestDataFactory.COURIER_PROFILE_ID, UUID.randomUUID(), "RO-B",
                AvailabilityStatus.AVAILABLE, userResponse(), null, null);
    }
}
