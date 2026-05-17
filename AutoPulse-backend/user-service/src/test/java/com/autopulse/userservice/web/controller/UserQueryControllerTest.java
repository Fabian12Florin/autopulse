package com.autopulse.userservice.web.controller;

import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.service.CourierService;
import com.autopulse.userservice.service.DispatcherService;
import com.autopulse.userservice.service.UserService;
import com.autopulse.userservice.testutil.TestDataFactory;
import com.autopulse.userservice.web.dto.CourierResponse;
import com.autopulse.userservice.web.dto.DispatcherResponse;
import com.autopulse.userservice.web.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserQueryControllerTest {

    private final UserService userService = mock(UserService.class);
    private final DispatcherService dispatcherService = mock(DispatcherService.class);
    private final CourierService courierService = mock(CourierService.class);
    private final UserQueryController controller = new UserQueryController(userService, dispatcherService, courierService);

    @Test
    void delegatesQueryEndpoints() {
        UserResponse user = new UserResponse(TestDataFactory.USER_ID, TestDataFactory.KEYCLOAK_ID,
                TestDataFactory.DISPATCHER_PROFILE_ID, TestDataFactory.COURIER_PROFILE_ID, "user@example.com",
                "Jane", "Driver", "+40722123456", true, null, null);
        DispatcherResponse dispatcher = new DispatcherResponse(TestDataFactory.DISPATCHER_PROFILE_ID, "RO-B", user, null, null);
        CourierResponse courier = new CourierResponse(TestDataFactory.COURIER_PROFILE_ID, TestDataFactory.DEPOT_ID, "RO-B",
                AvailabilityStatus.AVAILABLE, user, null, null);
        PageRequest pageable = PageRequest.of(0, 20);

        when(userService.search("u", "f", "l", "p", true, pageable)).thenReturn(new PageImpl<>(List.of(user)));
        when(dispatcherService.search("RO-B", true, pageable)).thenReturn(new PageImpl<>(List.of(dispatcher)));
        when(courierService.search(TestDataFactory.DEPOT_ID, "RO-B", AvailabilityStatus.AVAILABLE, true, pageable))
                .thenReturn(new PageImpl<>(List.of(courier)));
        when(courierService.searchAvailableByDepot(TestDataFactory.DEPOT_ID, pageable)).thenReturn(new PageImpl<>(List.of(courier)));
        when(courierService.getById(TestDataFactory.COURIER_PROFILE_ID)).thenReturn(courier);

        assertThat(controller.searchUsers("u", "f", "l", "p", true, pageable).getBody()).hasSize(1);
        assertThat(controller.searchDispatchers("RO-B", true, pageable).getBody()).hasSize(1);
        assertThat(controller.searchCouriers(TestDataFactory.DEPOT_ID, "RO-B", AvailabilityStatus.AVAILABLE, true, pageable).getBody()).hasSize(1);
        assertThat(controller.searchAvailableCouriersByDepot(TestDataFactory.DEPOT_ID, pageable).getBody()).hasSize(1);
        assertThat(controller.getCourier(TestDataFactory.COURIER_PROFILE_ID).getBody()).isSameAs(courier);
    }
}
