package uk.gov.pay.adminusers.resources;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InvalidEmailRequestExceptionMapper {

    @ExceptionHandler(InvalidEmailRequestException.class)
    public void toResponse(HttpServletResponse response) throws IOException {
        response.sendError(BAD_REQUEST.value());
    }
}
