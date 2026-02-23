package uk.gov.pay.adminusers.client.ledger.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.pay.adminusers.client.ledger.exception.LedgerException;
import uk.gov.pay.adminusers.client.ledger.model.LedgerSearchTransactionsResponse;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;

import java.net.URI;
import java.util.Optional;

import static java.lang.String.format;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.adminusers.utils.LoggingKeys.GATEWAY_ACCOUNT_ID;

public class LedgerService {

    private static final int SC_OK = 200;

    private final Logger logger = LoggerFactory.getLogger(LedgerService.class);

    private final RestClient client;
    private final String ledgerUrl;

    public LedgerService(RestClient client, String ledgerUrl) {
        this.client = client;
        this.ledgerUrl = ledgerUrl;
    }

    public Optional<LedgerTransaction> getTransaction(String id) {
        URI uri = UriComponentsBuilder
                .fromUriString(ledgerUrl)
                .path(format("/v1/transaction/%s", id))
                .queryParam("override_account_id_restriction", "true")
                .encode()
                .build()
                .toUri();

        logger.info("Querying ledger for transaction: {}", id);

        ResponseEntity<LedgerTransaction> response = getResponse(uri, LedgerTransaction.class);
        if (response.getStatusCode().value() == SC_OK) {
            return Optional.ofNullable(response.getBody());
        }
        return Optional.empty();
    }

    public LedgerSearchTransactionsResponse searchTransactions(String gatewayAccountId, int limit) {
        URI uri = UriComponentsBuilder
                .fromUriString(ledgerUrl)
                .path("/v1/transaction")
                .queryParam("account_id", gatewayAccountId)
                .queryParam("display_size", limit)
                .encode()
                .build()
                .toUri();

        logger.info("Searching transactions",
                kv(GATEWAY_ACCOUNT_ID, gatewayAccountId));

        ResponseEntity<LedgerSearchTransactionsResponse> response = getResponse(uri, LedgerSearchTransactionsResponse.class);
        if (response.getStatusCode().value() == SC_OK) {
            return response.getBody();
        }
        throw new LedgerException(uri, response.getStatusCode());
    }

    private <T> ResponseEntity<T> getResponse(URI uri, Class<T> type) {
        return client
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .exchange((request, response) -> {
                    HttpStatusCode status = response.getStatusCode();
                    T body = status.value() == SC_OK ? response.bodyTo(type) : null;
                    return ResponseEntity.status(status).headers(response.getHeaders()).body(body);
                });
    }
}
