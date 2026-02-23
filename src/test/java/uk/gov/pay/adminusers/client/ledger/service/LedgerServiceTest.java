package uk.gov.pay.adminusers.client.ledger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.gov.pay.adminusers.app.config.RestClientConfig;
import uk.gov.pay.adminusers.client.RestClientFactory;
import uk.gov.pay.adminusers.client.ledger.exception.LedgerException;
import uk.gov.pay.adminusers.client.ledger.model.LedgerSearchTransactionsResponse;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class LedgerServiceTest {

    private HttpClient httpClient;
    private MockRestServiceServer mockLedger;

    private LedgerService serviceUnderTest;

    private static final String LEDGER_URL = "http://ledgerUrl";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        httpClient = RestClientFactory.buildHttpClient(new RestClientConfig());
        RestClient.Builder clientBuilder = RestClientFactory.buildClient(httpClient).mutate();
        mockLedger = MockRestServiceServer.bindTo(clientBuilder).build();
        serviceUnderTest = new LedgerService(clientBuilder.build(), LEDGER_URL);
    }

    @AfterEach
    void tearDown() {
        httpClient.close();
    }

    @Test
    void getTransaction_shouldDeserialiseLedgerPaymentTransactionCorrectly() throws JsonProcessingException {
        String externalId = "e8eq11mi2ndmauvb51qsg8hccn";
        ImmutableMap<String, String> transactionData = ImmutableMap.of("transaction_id", externalId, "reference", "test event ref", "foo", "bar");
        String ledgerPayload = new Gson().toJson(transactionData);
        mockLedger.expect(requestTo(LEDGER_URL + "/v1/transaction/" + externalId + "?override_account_id_restriction=true"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(ledgerPayload));
        Optional<LedgerTransaction> mayBeTransaction = serviceUnderTest.getTransaction(externalId);
        assertThat(mayBeTransaction.isPresent(), is(true));
        LedgerTransaction transaction = mayBeTransaction.get();
        assertThat(transaction.getTransactionId(), is(externalId));
        assertThat(transaction.getReference(), is("test event ref"));
    }

    @Test
    void searchTransactions_shouldDeserialiseSearchTransactionsResponseCorrectly() throws JsonProcessingException {
        String externalId = "e8eq11mi2ndmauvb51qsg8hccn";
        Map<String, Object> transactionData = Map.of(
                "total", 1,
                "count", 1,
                "results", List.of(
                        Map.of("reference", "ref-1", "created_date", "2023-10-09T16:31:13.511Z",
                                "transaction_id", externalId, "gateway_account_id", "1")
                ));
        String ledgerPayload = objectMapper.writeValueAsString(transactionData);

        mockLedger.expect(requestTo(LEDGER_URL + "/v1/transaction?account_id=1&display_size=1"))
                .andExpect(method(GET))
                .andExpect(header(ACCEPT, APPLICATION_JSON_VALUE))
                .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(ledgerPayload));
        LedgerSearchTransactionsResponse ledgerSearchTransactionsResponse = serviceUnderTest.searchTransactions("1", 1);
        assertThat(ledgerSearchTransactionsResponse.getTransactions().size(), is(1));
        assertThat(ledgerSearchTransactionsResponse.getTransactions().get(0).getCreatedDate().toString(), is("2023-10-09T16:31:13.511Z"));
        assertThat(ledgerSearchTransactionsResponse.getTransactions().get(0).getTransactionId(), is("e8eq11mi2ndmauvb51qsg8hccn"));
    }

    @Test
    void searchTransactions_shouldThrowExceptionIfLedgerReturnsNon2xxError() throws JsonProcessingException {
        mockLedger.expect(requestTo(LEDGER_URL + "/v1/transaction?account_id=1&display_size=1"))
                .andRespond(withStatus(INTERNAL_SERVER_ERROR));

        assertThrows(LedgerException.class, () -> serviceUnderTest.searchTransactions("1", 1));
    }
}
