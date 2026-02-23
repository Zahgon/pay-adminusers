package uk.gov.pay.adminusers.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.gov.service.payments.commons.api.exception.ValidationException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionMapper {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, List<String>>> toResponse(ValidationException exception) {
        return ResponseEntity.status(BAD_REQUEST)
                .contentType(APPLICATION_JSON)
                .body(Map.of("errors", exception.getErrors()));
    }
}
