package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.adminusers.queue.model.ConnectorTask;
import uk.gov.pay.adminusers.queue.model.ServiceArchivedTaskData;

@ExtendWith(ProviderContractTestSuite.ConsumerSelection.class)
public class QueueMessageContractTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setTarget(PactVerificationContext context) {
        if (context != null) {
            context.setTarget(new MessageTestTarget());
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    public void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @PactVerifyProvider("a service archived event")
    public String verifyServiceArchivedEvent() throws JsonProcessingException {
        ConnectorTask event =
                new ConnectorTask(new ServiceArchivedTaskData("service-external-id"), "service_archived");
        return objectMapper.writeValueAsString(event);
    }
}
