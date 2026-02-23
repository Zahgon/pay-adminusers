package uk.gov.pay.adminusers.client;

import com.google.common.base.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.adminusers.utils.LoggingKeys.HTTP_STATUS;
import static uk.gov.pay.adminusers.utils.LoggingKeys.MDC_REQUEST_ID_KEY;
import static uk.gov.pay.adminusers.utils.LoggingKeys.METHOD;
import static uk.gov.pay.adminusers.utils.LoggingKeys.RESPONSE_TIME;
import static uk.gov.pay.adminusers.utils.LoggingKeys.URL;

/**
 * Passes the request id of the request being handled on to the called service ({@code X-Request-Id}, empty if there
 * is none) and logs the start and end of each call.
 */
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientLoggingInterceptor.class);
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        String requestId = StringUtils.defaultString(MDC.get(MDC_REQUEST_ID_KEY));
        request.getHeaders().add(HEADER_REQUEST_ID, requestId);
        LOGGER.info("[{}] - {} to {} began", requestId, request.getMethod(), request.getURI(),
                kv(METHOD, request.getMethod()),
                kv(URL, request.getURI()));
        ClientHttpResponse response = execution.execute(request, body);
        long elapsed = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
        LOGGER.info("[{}] - {} to {} ended - total time {}ms", requestId, request.getMethod(), request.getURI(), elapsed,
                kv(METHOD, request.getMethod()),
                kv(URL, request.getURI()),
                kv(HTTP_STATUS, response.getStatusCode().value()),
                kv(RESPONSE_TIME, elapsed));
        return response;
    }
}
