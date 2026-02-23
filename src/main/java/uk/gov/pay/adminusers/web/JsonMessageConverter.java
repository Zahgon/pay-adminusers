package uk.gov.pay.adminusers.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.Set;

/**
 * Reads and writes the API's JSON entities with the service's object mapper. A request entity is one JSON value
 * (anything after it makes the entity unreadable) of any content type whose subtype is {@code json},
 * {@code x-json}, {@code javascript}, {@code x-javascript} or ends with {@code +json}.
 */
public class JsonMessageConverter extends MappingJackson2HttpMessageConverter {

    private static final Set<String> READABLE_SUBTYPES = Set.of("json", "x-json", "javascript", "x-javascript");

    public JsonMessageConverter(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    public static boolean isReadable(MediaType mediaType) {
        String subtype = mediaType.getSubtype().toLowerCase();
        return READABLE_SUBTYPES.contains(subtype) || subtype.endsWith("+json");
    }

    @Override
    protected ObjectReader customizeReader(ObjectReader reader, JavaType javaType) {
        return reader.with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    @Override
    protected boolean canRead(MediaType mediaType) {
        return mediaType == null || isReadable(mediaType);
    }
}
