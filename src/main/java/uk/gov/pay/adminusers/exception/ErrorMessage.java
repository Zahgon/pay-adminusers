package uk.gov.pay.adminusers.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The entity of an error answer that is not specific to the service: the status code, a message and, for
 * unreadable JSON, the details of what could not be read.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorMessage(Integer code, String message, String details) {

    public ErrorMessage(int code, String message) {
        this(code, message, null);
    }
}
