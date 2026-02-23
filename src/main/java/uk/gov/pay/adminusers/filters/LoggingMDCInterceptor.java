package uk.gov.pay.adminusers.filters;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.Map;

import static uk.gov.pay.adminusers.utils.LoggingKeys.SERVICE_EXTERNAL_ID;
import static uk.gov.pay.adminusers.utils.LoggingKeys.USER_EXTERNAL_ID;

/**
 * Puts the service and user external ids named in the request path into the logging context while the request is
 * handled.
 */
public class LoggingMDCInterceptor implements HandlerInterceptor {

    private static final Map<String, String> PATH_VARIABLE_KEYS = Map.of(
            "serviceExternalId", SERVICE_EXTERNAL_ID,
            "userExternalId", USER_EXTERNAL_ID);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> variables) {
            PATH_VARIABLE_KEYS.forEach((variable, key) -> {
                if (variables.get(variable) instanceof String value) {
                    MDC.put(key, value);
                }
            });
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        List.of(SERVICE_EXTERNAL_ID, USER_EXTERNAL_ID).forEach(MDC::remove);
    }
}
