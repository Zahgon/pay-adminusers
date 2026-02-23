package uk.gov.pay.adminusers.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Every query parameter of the API takes a single value: when a request repeats one, its first value counts.
 */
public class FirstQueryParameterValueFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String[] getParameterValues(String name) {
                String[] values = super.getParameterValues(name);
                return values == null || values.length <= 1 ? values : new String[]{values[0]};
            }
        }, response);
    }
}
