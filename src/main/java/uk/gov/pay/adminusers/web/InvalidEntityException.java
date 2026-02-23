package uk.gov.pay.adminusers.web;

import java.util.List;

/**
 * A request entity that failed validation, with an error message per constraint it violates.
 */
public class InvalidEntityException extends RuntimeException {

    private final transient List<String> errors;

    public InvalidEntityException(List<String> errors) {
        super(String.join(", ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
