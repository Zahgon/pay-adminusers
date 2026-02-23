package uk.gov.pay.adminusers.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The HTTP methods the API declares for the resource a request path names. When several path templates match the
 * path, the resource is the one of the most specific template (literal segments before variables), whose methods
 * alone answer OPTIONS requests and requests with a method no matching template declares.
 */
@Component
public class ResourceMethods {

    private final ObjectProvider<RequestMappingHandlerMapping> handlerMapping;

    public ResourceMethods(ObjectProvider<RequestMappingHandlerMapping> handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    public List<String> declaredFor(HttpServletRequest request) {
        Map<String, List<RequestMappingInfo>> byPattern = handlerMapping.getObject("requestMappingHandlerMapping")
                .getHandlerMethods().keySet().stream()
                .filter(info -> info.getPathPatternsCondition() != null)
                .filter(info -> info.getPathPatternsCondition().getMatchingCondition(request) != null)
                .collect(Collectors.groupingBy(info -> info.getPathPatternsCondition().getFirstPattern().getPatternString()));
        Optional<List<RequestMappingInfo>> mostSpecific = byPattern.values().stream()
                .min(Comparator.comparing((List<RequestMappingInfo> infos) -> matching(infos, request),
                        (one, other) -> one.compareTo(other, request)));
        return mostSpecific.stream()
                .flatMap(List::stream)
                .flatMap(info -> info.getMethodsCondition().getMethods().stream())
                .map(Enum::name)
                .toList();
    }

    private static PathPatternsRequestCondition matching(List<RequestMappingInfo> infos, HttpServletRequest request) {
        return infos.getFirst().getPathPatternsCondition().getMatchingCondition(request);
    }
}
