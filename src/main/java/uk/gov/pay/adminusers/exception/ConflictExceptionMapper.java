package uk.gov.pay.adminusers.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConflictExceptionMapper {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, List<String>>> toResponse(ConflictException exception) {
        return ResponseEntity.status(CONFLICT)
                .contentType(APPLICATION_JSON)
                .body(Map.of("errors", List.of(exception.getMessage())));
    }
}
