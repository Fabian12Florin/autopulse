package com.autopulse.userservice.web.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChangePasswordRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsMatchingDifferentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123", "NewPass123", "NewPass123");

        assertThat(request.isConfirmationMatching()).isTrue();
        assertThat(request.isNewPasswordDifferent()).isTrue();
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMismatchAndSamePassword() {
        ChangePasswordRequest mismatch = new ChangePasswordRequest("OldPass123", "NewPass123", "OtherPass123");
        ChangePasswordRequest same = new ChangePasswordRequest("OldPass123", "OldPass123", "OldPass123");

        assertThat(mismatch.isConfirmationMatching()).isFalse();
        assertThat(same.isNewPasswordDifferent()).isFalse();
        assertThat(validator.validate(mismatch)).isNotEmpty();
        assertThat(validator.validate(same)).isNotEmpty();
    }

    @Test
    void nullBranchesAreHandledByBeanValidation() {
        ChangePasswordRequest request = new ChangePasswordRequest(null, null, null);

        assertThat(request.isConfirmationMatching()).isFalse();
        assertThat(request.isNewPasswordDifferent()).isTrue();
        assertThat(validator.validate(request)).isNotEmpty();
    }
}
