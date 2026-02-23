package uk.gov.pay.adminusers.web;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uk.gov.pay.adminusers.exception.ErrorMessage;
import uk.gov.pay.adminusers.exception.WebApplicationException;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Turns failures that are not specific to one resource into the answers clients know: routing and media type
 * errors, unreadable JSON and invalid request entities as JSON entities, {@link WebApplicationException}s with their
 * status and entity (or the standard error entity naming the status), statuses without an entity as the servlet
 * container's error page, and anything unexpected as a 500 naming the id under which it was logged.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiErrorHandler.class);

    private final ResourceMethods resourceMethods;

    public ApiErrorHandler(ResourceMethods resourceMethods) {
        this.resourceMethods = resourceMethods;
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorMessage> notFound() {
        return statusMessage(HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorMessage> methodNotAllowed(HttpServletRequest request) {
        Set<String> allowed = AllowedMethods.forUnsupportedMethod(resourceMethods.declaredFor(request));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.ALLOW, String.join(",", allowed))
                .contentType(APPLICATION_JSON)
                .body(message(HttpStatus.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorMessage> unsupportedMediaType() {
        return statusMessage(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorMessage> notAcceptable() {
        return statusMessage(HttpStatus.NOT_ACCEPTABLE);
    }

    /**
     * Malformed JSON, or JSON that does not fit the request entity, is a bad request whose details are Jackson's
     * own description of the problem; a request entity that cannot be read at all (the client went away) is a bad
     * request without an entity.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object unreadableEntity(HttpMessageNotReadableException exception, HttpServletResponse response)
            throws IOException {
        if (exception.getCause() instanceof JsonProcessingException jsonProcessingException) {
            if (jsonProcessingException instanceof JsonGenerationException
                    || jsonProcessingException instanceof InvalidDefinitionException) {
                return unexpected(jsonProcessingException);
            }
            LOGGER.debug("Unable to process JSON", jsonProcessingException);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(APPLICATION_JSON)
                    .body(new ErrorMessage(HttpStatus.BAD_REQUEST.value(), "Unable to process JSON",
                            jsonProcessingException.getOriginalMessage()));
        }
        LOGGER.debug("Unable to read the request entity", exception);
        response.sendError(HttpStatus.BAD_REQUEST.value());
        return null;
    }

    @ExceptionHandler(InvalidEntityException.class)
    public ResponseEntity<Map<String, Object>> invalidEntity(InvalidEntityException exception) {
        LOGGER.debug("Object validation failure", exception);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("errors", exception.getErrors()));
    }

    @ExceptionHandler(WebApplicationException.class)
    public ResponseEntity<Object> webApplicationException(WebApplicationException exception) {
        if (exception.getEntity() != null) {
            return ResponseEntity.status(exception.getStatus()).body(exception.getEntity());
        }
        if (exception.getStatus().is5xxServerError()) {
            logException(exception);
        }
        return ResponseEntity.status(exception.getStatus()).contentType(APPLICATION_JSON)
                .body(new ErrorMessage(exception.getStatus().value(), exception.getLocalizedMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public void statusWithoutEntity(ResponseStatusException exception, HttpServletResponse response) throws IOException {
        response.sendError(exception.getStatusCode().value());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpected(Exception exception) {
        long id = logException(exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(APPLICATION_JSON)
                .body(new ErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), format(Locale.ROOT,
                        "There was an error processing your request. It has been logged (ID %016x).", id)));
    }

    private static long logException(Exception exception) {
        long id = ThreadLocalRandom.current().nextLong();
        LOGGER.error(format(Locale.ROOT, "Error handling a request: %016x", id), exception);
        return id;
    }

    private static ResponseEntity<ErrorMessage> statusMessage(HttpStatus status) {
        return ResponseEntity.status(status).contentType(APPLICATION_JSON).body(message(status));
    }

    private static ErrorMessage message(HttpStatus status) {
        return new ErrorMessage(status.value(), format("HTTP %d %s", status.value(), status.getReasonPhrase()));
    }
}
