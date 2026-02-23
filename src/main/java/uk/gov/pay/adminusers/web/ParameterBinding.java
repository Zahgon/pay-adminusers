package uk.gov.pay.adminusers.web;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import uk.gov.pay.adminusers.exception.WebApplicationException;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.utils.Enums;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Parses query parameter values naming enum constants as the API always has: forgivingly (see
 * {@link Enums#fromStringFuzzy}), an empty value counting as absent, and an unknown value refused with 400 and
 * "{@code query param <name> must be one of [<constants>]}".
 */
@ControllerAdvice
public class ParameterBinding {

    @InitBinder
    public void registerParameterEditors(WebDataBinder binder) {
        binder.registerCustomEditor(RoleName.class, new FuzzyEnumEditor(RoleName.class, binder.getObjectName()));
    }

    private static class FuzzyEnumEditor extends PropertyEditorSupport {

        private final Enum<?>[] constants;
        private final String parameterName;

        FuzzyEnumEditor(Class<? extends Enum<?>> type, String parameterName) {
            this.constants = type.getEnumConstants();
            this.parameterName = parameterName;
        }

        @Override
        public void setAsText(String text) {
            if (text == null || text.isEmpty()) {
                setValue(null);
                return;
            }
            Enum<?> constant = Enums.fromStringFuzzy(text, constants);
            if (constant == null) {
                String constantsList = Arrays.stream(constants).map(Enum::toString).collect(Collectors.joining(", "));
                throw new WebApplicationException(
                        format("query param %s must be one of [%s]", parameterName, constantsList), BAD_REQUEST);
            }
            setValue(constant);
        }
    }
}
