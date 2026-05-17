package com.autopulse.userservice.service;

import com.autopulse.userservice.model.entity.CourierProfile;
import com.autopulse.userservice.model.entity.DispatcherProfile;
import com.autopulse.userservice.model.entity.User;

import java.util.UUID;

public interface CurrentUserService {

    UUID requireKeycloakUserId();

    String getCurrentUsername();

    boolean hasRole(String role);

    boolean hasAnyRole(String... roles);

    User requireLocalUser();

    DispatcherProfile requireCurrentDispatcherProfile();

    CourierProfile requireCurrentCourierProfile();
}
