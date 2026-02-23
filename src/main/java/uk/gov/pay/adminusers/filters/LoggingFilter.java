package uk.gov.pay.adminusers.filters;

import com.google.common.base.Stopwatch;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.concurrent.TimeUnit;

import static uk.gov.pay.adminusers.utils.LoggingKeys.MDC_REQUEST_ID_KEY;

/**
 * Logs the start and end of each API request and keeps the caller's {@code X-Request-Id} in the logging context, so
 * that it is passed on to the services adminusers calls. A failure that escapes the request handling is logged here
 * and goes no further.
 */
public class LoggingFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        String requestURL = request.getRequestURI();
        String requestMethod = request.getMethod();
        String requestId = request.getHeader(HEADER_REQUEST_ID);

        if (requestId == null) {
            MDC.remove(MDC_REQUEST_ID_KEY);
        } else {
            MDC.put(MDC_REQUEST_ID_KEY, requestId);
        }

        LOGGER.info("{} to {} began", requestMethod, requestURL);
        try {
            chain.doFilter(request, response);
        } catch (Throwable throwable) {
            LOGGER.error("Exception - {}", throwable.getMessage(), throwable);
        } finally {
            LOGGER.info("{} to {} ended - total time {}ms", requestMethod, requestURL,
                    stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
        }
    }
}
