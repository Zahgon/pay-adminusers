package uk.gov.pay.adminusers.exception;

import org.springframework.http.HttpStatus;

import static java.lang.String.format;

/**
 * Ends the handling of a request with the given status and, optionally, entity. Without an entity, the client sees
 * the standard error entity naming the status ({@code {"code":404,"message":"HTTP 404 Not Found"}}).
 */
public class WebApplicationException extends RuntimeException {

    private final HttpStatus status;
    private final Object entity;

    public WebApplicationException(HttpStatus status) {
        this(null, status, null);
    }

    public WebApplicationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.entity = null;
    }

    public WebApplicationException(Throwable cause, HttpStatus status, Object entity) {
        super(format("HTTP %d %s", status.value(), status.getReasonPhrase()), cause);
        this.status = status;
        this.entity = entity;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object getEntity() {
        return entity;
    }
}
