package uk.gov.pay.adminusers.web;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.ee10.servlet.ErrorHandler;
import org.eclipse.jetty.ee10.webapp.AbstractConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * The servlet container's own error pages, for answers that carry a status but no entity: an HTML page naming the
 * full request URL, the status and its reason, for GET, POST and HEAD requests only (other methods get the status
 * without an entity), as the service has always rendered them.
 */
@Component
public class ContainerErrorPages implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

    @Override
    public void customize(JettyServletWebServerFactory factory) {
        factory.addConfigurations(new AbstractConfiguration(new AbstractConfiguration.Builder()) {
            @Override
            public void configure(WebAppContext context) {
                context.setErrorHandler(new StatusPageErrorHandler());
            }
        });
        factory.addServerCustomizers(server -> {
            server.setErrorHandler(new RefusedRequestErrorHandler());
            server.setHandler(new FragmentRefusingHandler(server.getHandler()));
        });
    }

    /**
     * A request target with a fragment ({@code #...}) is a bad request.
     */
    static class FragmentRefusingHandler extends Handler.Wrapper {

        FragmentRefusingHandler(Handler handler) {
            super(handler);
        }

        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception {
            if (request.getHttpURI().getFragment() != null) {
                Response.writeError(request, response, callback, HttpStatus.BAD_REQUEST.value(), "Fragment");
                return true;
            }
            return super.handle(request, response, callback);
        }
    }

    /**
     * The error pages of requests refused before they reach the application (an ambiguous URI, say), which name the
     * request's path and query rather than its full URL.
     */
    static class RefusedRequestErrorHandler extends org.eclipse.jetty.server.handler.ErrorHandler {

        RefusedRequestErrorHandler() {
            setShowStacks(false);
        }

        @Override
        protected void writeErrorHtmlBody(Request request, Writer writer, int code, String message, Throwable cause,
                                          boolean showStacks) throws IOException {
            writeErrorHtmlMessage(request, writer, code, message, cause, target(request));
        }

        @Override
        protected void writeErrorPlain(Request request, PrintWriter writer, int code, String message, Throwable cause,
                                       boolean showStacks) {
            writer.write("HTTP ERROR " + code + " " + message + "\n");
            writer.write("URI: " + target(request) + "\n");
            writer.write("STATUS: " + code + "\n");
            writer.write("MESSAGE: " + message + "\n");
        }

        private static String target(Request request) {
            String fragment = request.getHttpURI().getFragment();
            return request.getHttpURI().getPathQuery() + (fragment == null ? "" : "#" + fragment);
        }
    }

    static class StatusPageErrorHandler extends ErrorHandler {

        StatusPageErrorHandler() {
            setShowServlet(false);
            setShowStacks(false);
        }

        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception {
            // Every error answer forbids caching, including the ones without an error page.
            response.getHeaders().put(HttpHeader.CACHE_CONTROL, getCacheControl());
            return super.handle(request, response, callback);
        }

        @Override
        protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message,
                                      boolean showStacks) throws IOException {
            super.writeErrorPage(request, writer, code, reason(code, message), showStacks);
        }

        @Override
        protected void writeErrorPageBody(HttpServletRequest request, Writer writer, int code, String message,
                                          boolean showStacks) throws IOException {
            writer.write("\n");
            writeErrorPageMessage(request, writer, code, message, requestUrl(request));
        }

        @Override
        protected void writeErrorPlain(HttpServletRequest request, PrintWriter writer, int code, String message) {
            writer.write("HTTP ERROR " + code + " " + reason(code, message) + "\n");
            writer.write("URI: " + requestUrl(request) + "\n");
            writer.write("STATUS: " + code + "\n");
            writer.write("MESSAGE: " + reason(code, message) + "\n");
        }

        @Override
        protected void writeErrorJson(HttpServletRequest request, PrintWriter writer, int code, String message) {
            writer.append("{\n\"message\":\"").append(jsonEscape(reason(code, message)))
                    .append("\",\n\"url\":\"").append(jsonEscape(requestUrl(request)))
                    .append("\",\n\"status\":\"").append(Integer.toString(code)).append("\"\n}");
        }

        /**
         * The message of a status the application set without one is the status's standard reason phrase.
         */
        private static String reason(int code, String message) {
            if (message == null || message.equals(org.eclipse.jetty.http.HttpStatus.getMessage(code))) {
                HttpStatus status = HttpStatus.resolve(code);
                return status == null ? message : status.getReasonPhrase();
            }
            return message;
        }

        private static String jsonEscape(String value) {
            StringBuilder out = new StringBuilder();
            for (char c : String.valueOf(value).toCharArray()) {
                if (c == '"' || c == '\\') {
                    out.append('\\').append(c);
                } else if (c < 0x20) {
                    out.append(String.format("\\u%04x", (int) c));
                } else {
                    out.append(c);
                }
            }
            return out.toString();
        }

        private static String requestUrl(HttpServletRequest request) {
            StringBuffer url = request.getRequestURL();
            if (request.getQueryString() != null) {
                url.append('?').append(request.getQueryString());
            }
            return url.toString();
        }
    }
}
