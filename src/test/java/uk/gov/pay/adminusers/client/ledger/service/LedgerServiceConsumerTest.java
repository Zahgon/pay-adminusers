package uk.gov.pay.adminusers.client.ledger.service;

import au.com.dius.pact.consumer.ConsumerPactRunnerKt;
import au.com.dius.pact.consumer.PactTestRun;
import au.com.dius.pact.consumer.PactVerificationResult;
import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.BasePact;
import au.com.dius.pact.core.model.DefaultPactReader;
import au.com.dius.pact.core.model.FileSource;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import uk.gov.pay.adminusers.app.config.RestClientConfig;
import uk.gov.pay.adminusers.client.RestClientFactory;
import uk.gov.pay.adminusers.client.ledger.model.LedgerSearchTransactionsResponse;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;

import java.io.File;
import java.net.http.HttpClient;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Runs the service's ledger client against pact's mock provider serving the interactions of the given pact files
 * ({@code pacts/<name>.json} on the class path), and fails unless every interaction was requested as specified.
 */
class LedgerServiceConsumerTest {

    @Test
    void getTransaction_shouldSerialiseLedgerPaymentTransactionCorrectly() {
        verifyPact("adminusers-ledger-get-payment-transaction", ledgerService -> {
            String externalId = "e8eq11mi2ndmauvb51qsg8hccn";
            Optional<LedgerTransaction> mayBeTransaction = ledgerService.getTransaction(externalId);

            assertThat(mayBeTransaction.isPresent(), is(true));
            LedgerTransaction transaction = mayBeTransaction.get();
            assertThat(transaction.getTransactionId(), is(externalId));
            assertThat(transaction.getReference(), is(notNullValue()));
        });
    }

    @Test
    void searchTransactions_shouldDeserialiseLedgerResponseCorrectly() {
        verifyPact("adminusers-ledger-search-transactions", ledgerService -> {
            String externalId = "e8eq11m3f2bg32dlll0dl2kdjg";
            LedgerSearchTransactionsResponse searchTransactionsResponse = ledgerService.searchTransactions("54322", 1);

            assertThat(searchTransactionsResponse.getTransactions().size(), is(1));
            LedgerTransaction transaction = searchTransactionsResponse.getTransactions().get(0);
            assertThat(transaction.getTransactionId(), is(externalId));
            assertThat(transaction.getCreatedDate(), is(notNullValue()));
        });
    }

    private interface LedgerServiceTest {
        void run(LedgerService ledgerService);
    }

    private static void verifyPact(String pactName, LedgerServiceTest test) {
        BasePact pact = (BasePact) DefaultPactReader.INSTANCE.loadPact(
                new FileSource(new File(Resources.getResource(String.format("pacts/%s.json", pactName)).getFile())));
        PactTestRun<Object> pactTestRun = (mockServer, context) -> {
            try (HttpClient httpClient = RestClientFactory.buildHttpClient(new RestClientConfig())) {
                test.run(new LedgerService(RestClientFactory.buildClient(httpClient), mockServer.getUrl()));
            }
            return null;
        };
        PactVerificationResult result = ConsumerPactRunnerKt.runConsumerTest(pact, MockProviderConfig.createDefault(),
                pactTestRun);
        if (result instanceof PactVerificationResult.Error error) {
            throw new AssertionError(error.getError());
        }
        assertThat(result.getDescription(), result, instanceOf(PactVerificationResult.Ok.class));
    }
}
