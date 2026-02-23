package uk.gov.pay.adminusers.validations;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates a bean by a {@code boolean is...()} method of it returning {@code true}. Its message is reported on its
 * own, without the name of the property.
 */
@Target({TYPE, ANNOTATION_TYPE, METHOD})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidationMethod.MethodValidator.class)
@Documented
public @interface ValidationMethod {

    String message() default "is not valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class MethodValidator implements ConstraintValidator<ValidationMethod, Boolean> {

        @Override
        public boolean isValid(Boolean value, ConstraintValidatorContext context) {
            return value == null || value;
        }
    }
}
