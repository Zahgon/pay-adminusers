package uk.gov.pay.adminusers.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.springframework.http.MediaType.TEXT_PLAIN;

/**
 * Answers OPTIONS requests for the API's resources: 200 with an {@code Allow} header and, when the client accepts
 * plain text, the same list as a {@code text/plain} entity; otherwise an empty entity of the first media type the
 * client asked for.
 */
public class OptionsInterceptor implements HandlerInterceptor {

    private final ResourceMethods resourceMethods;

    public OptionsInterceptor(ResourceMethods resourceMethods) {
        this.resourceMethods = resourceMethods;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!"OPTIONS".equals(request.getMethod())) {
            return true;
        }
        List<String> declared = resourceMethods.declaredFor(request);
        Set<String> allowed = AllowedMethods.forOptions(declared);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader(HttpHeaders.ALLOW, String.join(",", allowed));
        MediaType answerType = answerType(request.getHeader(HttpHeaders.ACCEPT));
        response.setContentType(answerType.toString());
        if (TEXT_PLAIN.equals(answerType)) {
            byte[] entity = String.join(", ", allowed).getBytes(StandardCharsets.UTF_8);
            response.setContentLength(entity.length);
            response.getOutputStream().write(entity);
        }
        return false;
    }

    private static MediaType answerType(String accept) {
        if (accept == null || accept.isBlank()) {
            return TEXT_PLAIN;
        }
        try {
            List<MediaType> accepted = MediaType.parseMediaTypes(accept);
            if (accepted.isEmpty() || accepted.stream().anyMatch(type -> type.includes(TEXT_PLAIN) && type.getQualityValue() > 0)) {
                return TEXT_PLAIN;
            }
            return accepted.getFirst().removeQualityValue();
        } catch (InvalidMediaTypeException e) {
            return TEXT_PLAIN;
        }
    }
}
