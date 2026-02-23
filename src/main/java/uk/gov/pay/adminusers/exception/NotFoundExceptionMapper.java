package uk.gov.pay.adminusers.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NotFoundExceptionMapper {

    @ExceptionHandler(NotFoundException.class)
    public void toResponse(HttpServletResponse response) throws IOException {
        response.sendError(NOT_FOUND.value());
    }
}
