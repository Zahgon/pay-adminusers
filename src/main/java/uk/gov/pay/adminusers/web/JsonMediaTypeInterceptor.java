package uk.gov.pay.adminusers.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Enforces {@link JsonMediaTypes} on the handler chosen for a request, before its arguments are read, in the order
 * the service always has:
 * <ol>
 *     <li>a content type that cannot be parsed (or is empty) is a bad request (400, no entity);</li>
 *     <li>an endpoint that consumes JSON refuses any other declared content type (415);</li>
 *     <li>a request whose {@code Accept} header excludes JSON is refused (406);</li>
 *     <li>an endpoint that reads a request entity needs a content type the JSON reader handles: {@code json},
 *     {@code x-json}, {@code javascript}, {@code x-javascript} or a {@code +json} subtype (415 otherwise, also when
 *     the request declares none).</li>
 * </ol>
 */
public class JsonMediaTypeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws HttpMediaTypeNotSupportedException, HttpMediaTypeNotAcceptableException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        JsonMediaTypes mediaTypes = handlerMethod.getMethodAnnotation(JsonMediaTypes.class);
        if (mediaTypes == null) {
            return true;
        }
        String contentTypeHeader = request.getHeader(HttpHeaders.CONTENT_TYPE);
        Optional<MediaType> contentType = parseContentType(contentTypeHeader);
        if (mediaTypes.consumes() && contentType.isPresent() && !isCompatibleWithJson(contentType.get())) {
            throw new HttpMediaTypeNotSupportedException(contentTypeHeader);
        }
        if (!producesAcceptable(request.getHeader(HttpHeaders.ACCEPT))) {
            throw new HttpMediaTypeNotAcceptableException(List.of(APPLICATION_JSON));
        }
        if (readsEntity(handlerMethod) && !contentType.map(JsonMessageConverter::isReadable).orElse(false)) {
            throw new HttpMediaTypeNotSupportedException(contentTypeHeader);
        }
        return true;
    }

    private static Optional<MediaType> parseContentType(String contentType) {
        if (contentType == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(MediaType.parseMediaType(contentType));
        } catch (InvalidMediaTypeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    private static boolean isCompatibleWithJson(MediaType mediaType) {
        return APPLICATION_JSON.isCompatibleWith(mediaType);
    }

    private static boolean readsEntity(HandlerMethod handlerMethod) {
        return Arrays.stream(handlerMethod.getMethodParameters()).anyMatch(JsonMediaTypeInterceptor::isRequestBody);
    }

    private static boolean isRequestBody(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestBody.class);
    }

    private static boolean producesAcceptable(String accept) {
        if (accept == null || accept.isBlank()) {
            return true;
        }
        try {
            return MediaType.parseMediaTypes(accept).stream()
                    .anyMatch(mediaType -> mediaType.includes(APPLICATION_JSON));
        } catch (InvalidMediaTypeException e) {
            // an Accept header that cannot be parsed accepts nothing (406)
            return false;
        }
    }
}
