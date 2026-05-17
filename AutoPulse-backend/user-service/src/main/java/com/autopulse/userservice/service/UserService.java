package com.autopulse.userservice.service;

import com.autopulse.userservice.web.dto.ChangePasswordRequest;
import com.autopulse.userservice.web.dto.PasswordChangeResponse;
import com.autopulse.userservice.web.dto.PasswordResetAcceptedResponse;
import com.autopulse.userservice.web.dto.PasswordResetRequest;
import com.autopulse.userservice.web.dto.PasswordResetResponse;
import com.autopulse.userservice.web.dto.UpdateMyUserRequest;
import com.autopulse.userservice.web.dto.UpdateUserRequest;
import com.autopulse.userservice.web.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse getById(UUID userId);

    UserResponse getCurrentUser();

    Page<UserResponse> search(String email,
                              String firstName,
                              String lastName,
                              String phoneNumber,
                              Boolean active,
                              Pageable pageable);

    UserResponse update(UUID userId, UpdateUserRequest request);

    UserResponse updateCurrentUser(UpdateMyUserRequest request);

    UserResponse activate(UUID userId);

    UserResponse deactivate(UUID userId);

    PasswordResetResponse resetPassword(UUID userId);

    PasswordResetAcceptedResponse requestPasswordReset(PasswordResetRequest request);

    PasswordChangeResponse changeMyPassword(ChangePasswordRequest request);
}
