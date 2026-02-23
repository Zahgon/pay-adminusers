package uk.gov.pay.adminusers.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.HttpEncodingAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import uk.gov.pay.adminusers.exception.ConflictExceptionMapper;
import uk.gov.pay.adminusers.exception.NotFoundExceptionMapper;
import uk.gov.pay.adminusers.exception.ValidationExceptionMapper;
import uk.gov.pay.adminusers.resources.InvalidEmailRequestExceptionMapper;
import uk.gov.pay.adminusers.resources.InvalidMerchantDetailsExceptionMapper;
import uk.gov.pay.adminusers.utils.json.ObjectMapperFactory;
import uk.gov.pay.adminusers.web.ApiErrorHandler;
import uk.gov.pay.adminusers.web.ContainerErrorPages;
import uk.gov.pay.adminusers.web.JsonMessageConverter;
import uk.gov.pay.adminusers.web.ParameterBinding;
import uk.gov.pay.adminusers.web.RequestEntityValidation;
import uk.gov.pay.adminusers.web.ResourceMethods;
import uk.gov.pay.adminusers.web.WebConfiguration;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * Serves the given resources (controller instances, typically wired with mocks) through the service's web layer
 * (its message conversion, request validation, exception mappers, filters and error pages) on a random port for the
 * tests of a class, as Dropwizard's {@code ResourceExtension} did with Jersey.
 */
public class ResourceExtension implements BeforeAllCallback, AfterAllCallback {

    private final List<Object> resources;
    private ConfigurableApplicationContext applicationContext;

    private ResourceExtension(List<Object> resources) {
        this.resources = List.copyOf(resources);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        applicationContext = new SpringApplicationBuilder(WebLayerConfiguration.class)
                .initializers(applicationContext -> {
                    for (int i = 0; i < resources.size(); i++) {
                        applicationContext.getBeanFactory().registerSingleton("resource" + i, resources.get(i));
                    }
                })
                .run("--server.port=0", "--server.address=127.0.0.1", "--logging.level.root=ERROR");
    }

    @Override
    public void afterAll(ExtensionContext context) {
        applicationContext.close();
    }

    /**
     * A request to the resources, to which a path and method are still to be given.
     */
    public RequestSpecification target() {
        return given().port(((WebServerApplicationContext) applicationContext).getWebServer().getPort());
    }

    public static class Builder {
        private final List<Object> resources = new ArrayList<>();

        public Builder addResource(Object resource) {
            resources.add(resource);
            return this;
        }

        public ResourceExtension build() {
            return new ResourceExtension(resources);
        }
    }

    /**
     * The service's web layer without its resources. Not a {@code @Configuration}, so that the service's component
     * scan, which covers the test classes too, leaves it out.
     */
    @ImportAutoConfiguration({PropertyPlaceholderAutoConfiguration.class, ServletWebServerFactoryAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class, HttpEncodingAutoConfiguration.class,
            ValidationAutoConfiguration.class})
    @Import({WebConfiguration.class, ResourceMethods.class, ApiErrorHandler.class, ParameterBinding.class,
            RequestEntityValidation.class, ContainerErrorPages.class, NotFoundExceptionMapper.class,
            ConflictExceptionMapper.class, ValidationExceptionMapper.class, InvalidEmailRequestExceptionMapper.class,
            InvalidMerchantDetailsExceptionMapper.class})
    static class WebLayerConfiguration {

        @Bean
        @Primary
        public ObjectMapper objectMapper() {
            return ObjectMapperFactory.newObjectMapper();
        }

        @Bean
        public JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
            return new JsonMessageConverter(objectMapper);
        }
    }
}
