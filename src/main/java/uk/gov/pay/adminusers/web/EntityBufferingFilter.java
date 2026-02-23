package uk.gov.pay.adminusers.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Holds back response entities of up to {@value #BUFFER_SIZE} bytes until the request has been handled, so that
 * they are sent with a {@code Content-Length}; larger entities are streamed as soon as they outgrow the buffer.
 */
public class EntityBufferingFilter extends OncePerRequestFilter {

    private static final int BUFFER_SIZE = 8192;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        BufferingResponse bufferingResponse = new BufferingResponse(response);
        try {
            chain.doFilter(request, bufferingResponse);
        } finally {
            bufferingResponse.complete();
        }
    }

    private static final class BufferingResponse extends HttpServletResponseWrapper {

        private final BufferingOutputStream outputStream;
        private PrintWriter writer;

        private BufferingResponse(HttpServletResponse response) {
            super(response);
            this.outputStream = new BufferingOutputStream(response);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public PrintWriter getWriter() {
            if (writer == null) {
                String encoding = getCharacterEncoding() == null ? StandardCharsets.ISO_8859_1.name() : getCharacterEncoding();
                writer = new PrintWriter(new OutputStreamWriter(outputStream, Charset.forName(encoding)));
            }
            return writer;
        }

        @Override
        public void sendError(int sc) throws IOException {
            outputStream.discard();
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            outputStream.discard();
            super.sendError(sc, msg);
        }

        @Override
        public void flushBuffer() throws IOException {
            if (writer != null) {
                writer.flush();
            }
        }

        @Override
        public void resetBuffer() {
            outputStream.discard();
            super.resetBuffer();
        }

        @Override
        public void reset() {
            outputStream.discard();
            super.reset();
        }

        private void complete() throws IOException {
            if (writer != null) {
                writer.flush();
            }
            outputStream.complete();
        }
    }

    private static final class BufferingOutputStream extends ServletOutputStream {

        private final HttpServletResponse response;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private boolean streaming;

        private BufferingOutputStream(HttpServletResponse response) {
            this.response = response;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            if (streaming) {
                response.getOutputStream().write(bytes, offset, length);
                return;
            }
            buffer.write(bytes, offset, length);
            if (buffer.size() > BUFFER_SIZE) {
                streaming = true;
                buffer.writeTo(response.getOutputStream());
                buffer.reset();
            }
        }

        /**
         * The entity is sent once complete, or as soon as it outgrows the buffer: only then do flushes reach the
         * client.
         */
        @Override
        public void flush() throws IOException {
            if (streaming) {
                response.getOutputStream().flush();
            }
        }

        private void discard() {
            buffer.reset();
        }

        private void complete() throws IOException {
            if (!streaming && buffer.size() > 0 && !response.isCommitted()) {
                response.setContentLength(buffer.size());
                buffer.writeTo(response.getOutputStream());
                buffer.reset();
            }
        }

        @Override
        public boolean isReady() {
            try {
                return !streaming || response.getOutputStream().isReady();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            try {
                response.getOutputStream().setWriteListener(writeListener);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
