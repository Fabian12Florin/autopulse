package com.autopulse.common.kafka.event;

import java.util.UUID;

public record UserResetPasswordEvent(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String password,
        String role
) {
}
