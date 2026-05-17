package com.autopulse.userservice.service;

import com.autopulse.userservice.model.entity.User;

public interface CredentialNotificationService {

    void sendInitialPassword(User user, String role, String temporaryPassword);

    void sendResetPassword(User user, String temporaryPassword);
}
