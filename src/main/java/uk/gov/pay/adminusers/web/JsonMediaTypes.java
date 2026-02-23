package uk.gov.pay.adminusers.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a handler method that answers with JSON and, when {@link #consumes()} is set, only accepts JSON requests.
 * {@link JsonMediaTypeInterceptor} enforces it the way the service always has.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonMediaTypes {

    /**
     * @return whether the endpoint only accepts requests that declare a JSON content type (or none)
     */
    boolean consumes() default false;
}
