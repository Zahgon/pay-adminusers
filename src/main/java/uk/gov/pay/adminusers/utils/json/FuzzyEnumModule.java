package uk.gov.pay.adminusers.utils.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import uk.gov.pay.adminusers.utils.Enums;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads enums that have no Jackson annotations of their own forgivingly (see {@link Enums#fromStringFuzzy}): an
 * unknown value fails with "{@code <value> was not one of [<constants>]}".
 */
public class FuzzyEnumModule extends Module {

    @Override
    public String getModuleName() {
        return "permissive-enums";
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addDeserializers(new PermissiveEnumDeserializers());
    }

    private static class PermissiveEnumDeserializer extends StdScalarDeserializer<Enum<?>> {

        private static final long serialVersionUID = 1L;

        private final Enum<?>[] constants;
        private final List<String> acceptedValues;

        @SuppressWarnings("unchecked")
        PermissiveEnumDeserializer(Class<Enum<?>> clazz) {
            super(clazz);
            this.constants = ((Class<Enum<?>>) handledType()).getEnumConstants();
            this.acceptedValues = new ArrayList<>();
            for (Enum<?> constant : constants) {
                acceptedValues.add(constant.name());
            }
        }

        @Override
        public Enum<?> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            Enum<?> constant = Enums.fromStringFuzzy(parser.getText(), constants);
            if (constant != null) {
                return constant;
            }
            throw context.weirdStringException(parser.getText(), handledType(),
                    parser.getText() + " was not one of " + acceptedValues);
        }

        @Override
        public boolean isCachable() {
            return true;
        }
    }

    private static class PermissiveEnumDeserializers extends Deserializers.Base {

        @Override
        @SuppressWarnings("unchecked")
        public JsonDeserializer<?> findEnumDeserializer(Class<?> type, DeserializationConfig config,
                                                        BeanDescription description) {
            if (config.hasDeserializationFeatures(DeserializationFeature.READ_ENUMS_USING_TO_STRING.getMask())
                    || config.hasDeserializationFeatures(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL.getMask())
                    || config.hasDeserializationFeatures(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE.getMask())) {
                return null;
            }
            for (AnnotatedMethod factoryMethod : description.getFactoryMethods()) {
                if (factoryMethod.hasAnnotation(JsonCreator.class)) {
                    return null;
                }
            }
            for (Field field : type.getFields()) {
                for (Annotation annotation : field.getAnnotations()) {
                    if (annotation.annotationType().getPackage().getName().equals("com.fasterxml.jackson.annotation")) {
                        return null;
                    }
                }
            }
            return new PermissiveEnumDeserializer((Class<Enum<?>>) type);
        }
    }
}
