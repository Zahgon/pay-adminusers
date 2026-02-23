package uk.gov.pay.adminusers.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A request entity declared in a character set the JVM does not know is read as if no character set had been
 * declared (JSON's own encoding detection then applies).
 */
public class UnknownCharsetFilter extends OncePerRequestFilter {

    private static final Pattern CHARSET_PARAMETER = Pattern.compile(";\\s*charset\\s*=\\s*\"?([^;\"]*)\"?\\s*", Pattern.CASE_INSENSITIVE);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        Matcher charset = contentType == null ? null : CHARSET_PARAMETER.matcher(contentType);
        if (charset == null || !charset.find() || isSupported(charset.group(1).trim())) {
            chain.doFilter(request, response);
            return;
        }
        String withoutCharset = charset.replaceFirst("");
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getContentType() {
                return withoutCharset;
            }

            @Override
            public String getCharacterEncoding() {
                return null;
            }

            @Override
            public String getHeader(String name) {
                return HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name) ? withoutCharset : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                return HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)
                        ? Collections.enumeration(List.of(withoutCharset)) : super.getHeaders(name);
            }
        }, response);
    }

    private static boolean isSupported(String charset) {
        try {
            return Charset.isSupported(charset);
        } catch (IllegalCharsetNameException e) {
            // a charset whose name is not even legal is as unknown as an unsupported one
            return false;
        }
    }
}
