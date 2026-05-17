package com.autopulse.userservice.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,
        @NotBlank(message = "First name must not be blank")
        @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
        String firstName,
        @NotBlank(message = "Last name must not be blank")
        @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
        String lastName,
        @NotBlank(message = "Phone number must not be blank")
        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Phone number must be in E.164-like format")
        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phoneNumber,
        boolean active
) {
}
