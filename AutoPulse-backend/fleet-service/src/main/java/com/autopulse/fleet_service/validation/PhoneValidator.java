package com.autopulse.fleet_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<Phone, String> {

    private static final String ROMANIAN_PHONE_REGEX =
            "^(\\+4|004)?(07\\d{2}|02\\d{2}|03\\d{2})[\\s.-]?\\d{3}[\\s.-]?\\d{3}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null
                && !value.isBlank()
                && value.trim().matches(ROMANIAN_PHONE_REGEX);
    }
}