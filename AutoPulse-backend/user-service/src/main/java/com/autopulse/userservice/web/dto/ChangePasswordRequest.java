package com.autopulse.userservice.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password must not be blank")
        @Size(max = 255, message = "Current password must not exceed 255 characters")
        String currentPassword,

        @NotBlank(message = "New password must not be blank")
        @Size(min = 8, max = 255, message = "New password must be between 8 and 255 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "New password must contain at least one lowercase letter, one uppercase letter, and one digit"
        )
        String newPassword,

        @NotBlank(message = "New password confirmation must not be blank")
        @Size(min = 8, max = 255, message = "New password confirmation must be between 8 and 255 characters")
        String confirmNewPassword
) {

    @AssertTrue(message = "New password confirmation must match new password")
    public boolean isConfirmationMatching() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }

    @AssertTrue(message = "New password must be different from current password")
    public boolean isNewPasswordDifferent() {
        return currentPassword == null || newPassword == null || !currentPassword.equals(newPassword);
    }
}
