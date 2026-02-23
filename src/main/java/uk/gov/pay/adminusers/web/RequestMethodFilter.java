package uk.gov.pay.adminusers.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Only the methods the API's resources can declare reach them: TRACE and any extension method are refused by the
 * servlet container with 405 and no entity.
 */
public class RequestMethodFilter extends OncePerRequestFilter {

    private static final Set<String> RESOURCE_METHODS = Set.of("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!RESOURCE_METHODS.contains(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        chain.doFilter(request, response);
    }
}
