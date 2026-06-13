package com.isd.wms.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#])[A-Za-z\\d@$!%*?&_#]{8,64}$"
    );

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        log.trace("Password complexity validation invoked.");
        if (password == null || password.isBlank()) {
            log.warn("Password validation failed: Provided value is null, empty or blank.");
            return false;
        }

        int passwordLength = password.length();
        boolean matchesRegex = PATTERN.matcher(password).matches();

        if (!matchesRegex) {
            if (passwordLength < 8 || passwordLength > 64) {
                log.warn("Password validation failed: Length violation. Current length: {} characters (Allowed: 8-64).", passwordLength);
            } else {
                log.warn("Password validation failed: Complexity rules violated (Must include: lowercase, uppercase, digit, special char). Length is valid: {} chars.", passwordLength);
            }
            return false;
        }

        log.debug("Password complexity check passed successfully. Length: {} chars.", passwordLength);
        return true;
    }
}
