package uk.gov.pay.adminusers.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import uk.gov.pay.adminusers.validations.ValidationMethod;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Validates request entities annotated {@link Valid} or {@link NotNull} as soon as they are read, against the
 * constraints of the handler method's parameter, and refuses an invalid one with {@link InvalidEntityException}
 * (422). Each error names the property that failed followed by the constraint's message ("email must not be
 * empty"), the request body itself when the entity is missing ("The request body must not be null"), or is only
 * the message of a {@link ValidationMethod}. The constraints are checked on the controller that handles the request,
 * which this class, registered as an interceptor too, takes from the handler Spring resolved for the request.
 */
@ControllerAdvice
public class RequestEntityValidation extends RequestBodyAdviceAdapter implements HandlerInterceptor {

    private static final String REQUEST_BODY = "The request body";
    private static final String CONTROLLER_ATTRIBUTE = RequestEntityValidation.class.getName() + ".controller";

    private final Validator validator;
    private final HttpServletRequest request;

    public RequestEntityValidation(Validator validator, HttpServletRequest request) {
        this.validator = validator;
        this.request = request;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            request.setAttribute(CONTROLLER_ATTRIBUTE, handlerMethod.getBean());
        }
        return true;
    }

    @Override
    public boolean supports(MethodParameter parameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return parameter.hasParameterAnnotation(Valid.class) || parameter.hasParameterAnnotation(NotNull.class);
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        validate(body, parameter);
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                  Class<? extends HttpMessageConverter<?>> converterType) {
        validate(body, parameter);
        return body;
    }

    private void validate(Object entity, MethodParameter parameter) {
        Method method = parameter.getMethod();
        Object[] arguments = new Object[method.getParameterCount()];
        arguments[parameter.getParameterIndex()] = entity;
        Object controller = request.getAttribute(CONTROLLER_ATTRIBUTE);
        Set<ConstraintViolation<Object>> violations =
                validator.forExecutables().validateParameters(controller, method, arguments);
        if (!violations.isEmpty()) {
            throw new InvalidEntityException(violations.stream().map(RequestEntityValidation::message).toList());
        }
    }

    private static String message(ConstraintViolation<?> violation) {
        if (violation.getConstraintDescriptor().getAnnotation() instanceof ValidationMethod) {
            return violation.getMessage();
        }
        List<Path.Node> nodes = StreamSupport.stream(violation.getPropertyPath().spliterator(), false).toList();
        String property = nodes.stream().skip(2).map(Path.Node::toString).collect(Collectors.joining("."));
        return (property.isEmpty() ? REQUEST_BODY : property) + " " + violation.getMessage();
    }
}
