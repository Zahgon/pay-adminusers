package uk.gov.pay.adminusers.client.ledger.exception;

import org.springframework.http.HttpStatusCode;

import java.net.URI;

import static java.lang.String.format;

public class LedgerException extends RuntimeException {

    private Integer status;

    public LedgerException(URI uri, HttpStatusCode status) {
        super(format("GET %s answered %d", uri, status.value()));
        this.status = status.value();
    }

    public LedgerException(Exception exception) {
        super(exception);
    }

    @Override
    public String toString() {
        return "LedgerException{" +
                "status=" + status +
                ", message=" + getMessage() +
                '}';
    }
}
