package com.autopulse.userservice.web.controller;

import com.autopulse.userservice.model.enums.AvailabilityStatus;
import com.autopulse.userservice.service.CourierService;
import com.autopulse.userservice.service.DispatcherService;
import com.autopulse.userservice.testutil.TestDataFactory;
import com.autopulse.userservice.web.dto.CourierResponse;
import com.autopulse.userservice.web.dto.DispatcherResponse;
import com.autopulse.userservice.web.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DispatcherControllerTest {

    private final DispatcherService dispatcherService = mock(DispatcherService.class);
    private final CourierService courierService = mock(CourierService.class);
    private final DispatcherController controller = new DispatcherController(dispatcherService, courierService);

    @Test
    void delegatesDispatcherCourierEndpoints() {
        UserResponse user = userResponse();
        DispatcherResponse dispatcher = new DispatcherResponse(TestDataFactory.DISPATCHER_PROFILE_ID, "RO-B", user, null, null);
        CourierResponse courier = new CourierResponse(TestDataFactory.COURIER_PROFILE_ID, TestDataFactory.DEPOT_ID, "RO-B",
                AvailabilityStatus.AVAILABLE, user, null, null);
        PageRequest pageable = PageRequest.of(0, 20);

        when(dispatcherService.getById(TestDataFactory.DISPATCHER_PROFILE_ID)).thenReturn(dispatcher);
        when(courierService.search(TestDataFactory.DEPOT_ID, "RO-B", AvailabilityStatus.AVAILABLE, true, pageable))
                .thenReturn(new PageImpl<>(List.of(courier)));
        when(courierService.getById(TestDataFactory.COURIER_PROFILE_ID)).thenReturn(courier);
        when(courierService.create(TestDataFactory.createCourierRequest(AvailabilityStatus.AVAILABLE, "RO-B"))).thenReturn(courier);
        when(courierService.update(TestDataFactory.COURIER_PROFILE_ID, TestDataFactory.updateCourierRequest(AvailabilityStatus.AVAILABLE, true, "RO-B"))).thenReturn(courier);

        assertThat(controller.getDispatcher(TestDataFactory.DISPATCHER_PROFILE_ID).getBody()).isSameAs(dispatcher);
        assertThat(controller.searchCouriers(TestDataFactory.DEPOT_ID, "RO-B", AvailabilityStatus.AVAILABLE, true, pageable).getBody()).hasSize(1);
        assertThat(controller.getCourier(TestDataFactory.COURIER_PROFILE_ID).getBody()).isSameAs(courier);
        assertThat(controller.createCourier(TestDataFactory.createCourierRequest(AvailabilityStatus.AVAILABLE, "RO-B")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateCourier(TestDataFactory.COURIER_PROFILE_ID, TestDataFactory.updateCourierRequest(AvailabilityStatus.AVAILABLE, true, "RO-B")).getBody()).isSameAs(courier);
    }

    private UserResponse userResponse() {
        return new UserResponse(TestDataFactory.USER_ID, TestDataFactory.KEYCLOAK_ID,
                TestDataFactory.DISPATCHER_PROFILE_ID, TestDataFactory.COURIER_PROFILE_ID, "user@example.com",
                "Jane", "Driver", "+40722123456", true, null, null);
    }
}
