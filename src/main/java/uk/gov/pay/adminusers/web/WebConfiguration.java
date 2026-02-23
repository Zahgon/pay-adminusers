package uk.gov.pay.adminusers.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.UrlHandlerFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.pay.adminusers.filters.LoggingFilter;
import uk.gov.pay.adminusers.filters.LoggingMDCInterceptor;

import java.io.IOException;

/**
 * Spring MVC settings that keep the API's request handling as clients know it: OPTIONS answers, JSON media type
 * checks, repeated query parameters, trailing slashes and request logging.
 */
@Configuration(proxyBeanMethods = false)
public class WebConfiguration implements WebMvcConfigurer {

    private final ResourceMethods resourceMethods;
    private final RequestEntityValidation requestEntityValidation;

    public WebConfiguration(ResourceMethods resourceMethods, RequestEntityValidation requestEntityValidation) {
        this.resourceMethods = resourceMethods;
        this.requestEntityValidation = requestEntityValidation;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new OptionsInterceptor(resourceMethods));
        registry.addInterceptor(new JsonMediaTypeInterceptor());
        registry.addInterceptor(new LoggingMDCInterceptor());
        registry.addInterceptor(requestEntityValidation);
    }

    /**
     * OPTIONS requests are answered by the API alone: an OPTIONS request for a path the API does not know is not
     * found, without an {@code Allow} header listing every HTTP method.
     */
    @Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
    public DispatcherServlet dispatcherServlet() {
        DispatcherServlet dispatcherServlet = new DispatcherServlet() {
            @Override
            protected void doOptions(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                processRequest(request, response);
            }
        };
        dispatcherServlet.setDispatchOptionsRequest(true);
        return dispatcherServlet;
    }

    @Bean
    public FilterRegistrationBean<LoggingFilter> loggingFilter() {
        FilterRegistrationBean<LoggingFilter> registration = new FilterRegistrationBean<>(new LoggingFilter());
        registration.addUrlPatterns("/v1/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestMethodFilter> requestMethodFilter() {
        FilterRegistrationBean<RequestMethodFilter> registration = new FilterRegistrationBean<>(new RequestMethodFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<FirstQueryParameterValueFilter> firstQueryParameterValueFilter() {
        FilterRegistrationBean<FirstQueryParameterValueFilter> registration =
                new FilterRegistrationBean<>(new FirstQueryParameterValueFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
        FilterRegistrationBean<UrlHandlerFilter> registration =
                new FilterRegistrationBean<>(UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 3);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<UnknownCharsetFilter> unknownCharsetFilter() {
        FilterRegistrationBean<UnknownCharsetFilter> registration = new FilterRegistrationBean<>(new UnknownCharsetFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<EntityBufferingFilter> entityBufferingFilter() {
        FilterRegistrationBean<EntityBufferingFilter> registration = new FilterRegistrationBean<>(new EntityBufferingFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 4);
        return registration;
    }
}
