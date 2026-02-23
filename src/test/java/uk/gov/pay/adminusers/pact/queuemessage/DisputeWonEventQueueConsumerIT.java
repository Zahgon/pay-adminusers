package uk.gov.pay.adminusers.pact.queuemessage;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import uk.gov.pay.adminusers.client.ledger.model.LedgerTransaction;
import uk.gov.pay.adminusers.fixtures.EventFixture;
import uk.gov.pay.adminusers.fixtures.ServiceDbFixture;
import uk.gov.pay.adminusers.infra.AppWithPostgresAndSqsExtension;
import uk.gov.pay.adminusers.infra.LedgerStub;
import uk.gov.pay.adminusers.infra.NotifyStub;
import uk.gov.pay.adminusers.model.MerchantDetails;
import uk.gov.pay.adminusers.model.Role;
import uk.gov.pay.adminusers.model.RoleName;
import uk.gov.pay.adminusers.model.Service;
import uk.gov.pay.adminusers.persistence.dao.RoleDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.pay.adminusers.fixtures.EventFixture.anEventFixture;
import static uk.gov.pay.adminusers.fixtures.LedgerTransactionFixture.aLedgerTransactionFixture;
import static uk.gov.pay.adminusers.fixtures.UserDbFixture.userDbFixture;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "connector", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
public class DisputeWonEventQueueConsumerIT {
    
    @RegisterExtension
    public AppWithPostgresAndSqsExtension adminusersApp = new AppWithPostgresAndSqsExtension(
            "adminusers.event-subscriber-queue-config.event-subscriber-queue-enabled", "true");

    public WireMockServer wireMockRule = new WireMockServer(options().port(adminusersApp.getWireMockPort()));
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private byte[] currentMessage;
    private final String gatewayAccountId = "a-gateway-account-id";
    private final String resourceExternalId = "dispute-id";
    private final String parentResourceExternalId = "payment-id";
    private final String reference = "REF123";
    private final String serviceId = "service-id";
    private final String serviceName = "A service";
    private final String organisationName = "organisation name";;
    private final String adminUserEmail = "user@example.com";

    private EventFixture eventFixture;

    private LedgerStub ledgerStub;
    private NotifyStub notifyStub;

    @Pact(provider = "connector", consumer = "adminusers")
    public MessagePact createEvidenceSubmittedEventPact(MessagePactBuilder builder) {
        JsonNode eventDetails = objectMapper.valueToTree(Map.of(
                        "gateway_account_id", gatewayAccountId
                ));
        
        eventFixture = anEventFixture()
                .withLive(true)
                .withResourceExternalId(resourceExternalId)
                .withParentResourceExternalId(parentResourceExternalId)
                .withServiceId(serviceId)
                .withEventType("DISPUTE_WON")
                .withEventDetails(eventDetails);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("contentType", "application/json");

        return builder
                .expectsToReceive("a dispute won event")
                .withMetadata(metadata)
                .withContent(eventFixture.getAsPact())
                .toPact();
    }

    @BeforeEach
    public void setUp() throws Exception {
        wireMockRule.start();
        adminusersApp.getDatabaseTestHelper().truncateAllData();
        
        ledgerStub = new LedgerStub(wireMockRule);
        notifyStub = new NotifyStub(wireMockRule);

        Service service = ServiceDbFixture.serviceDbFixture(adminusersApp.getDatabaseTestHelper())
                .withGatewayAccountIds(gatewayAccountId)
                .withName(serviceName)
                .withMerchantDetails(new MerchantDetails(
                        organisationName, "number", "line1", null, "city",
                        "postcode", "country", "dd-merchant@example.com",
                        "https://merchant.example.org"
                ))
                .insertService();
        Role adminRole = adminusersApp.getBean(RoleDao.class).findByRoleName(RoleName.ADMIN).get().toRole();
        userDbFixture(adminusersApp.getDatabaseTestHelper())
                .withEmail(adminUserEmail)
                .withServiceRole(service.getId(), adminRole)
                .insertUser();

        LedgerTransaction ledgerTransaction = aLedgerTransactionFixture()
                .withTransactionId(parentResourceExternalId)
                .withReference(reference)
                .build();
        ledgerStub.returnLedgerTransaction(parentResourceExternalId, ledgerTransaction);
        notifyStub.stubSendEmail();
    }

    @AfterEach
    public void tearDown() {
        wireMockRule.stop();
    }

    @Test
    @PactTestFor(pactMethod = "createEvidenceSubmittedEventPact")
    public void test(List<Message> messages) throws Exception {
        setMessage(messages.get(0).contentsAsBytes());
        String messageContents = new String(currentMessage);
        String snsMessage = new GsonBuilder().create().toJson(Map.of("Message", messageContents));
        SendMessageRequest messageRequest = SendMessageRequest.builder()
                .queueUrl(adminusersApp.getQueueUrl("event-queue"))
                .messageBody(snsMessage)
                .build();
        adminusersApp.getSqsClient().sendMessage(messageRequest);
        await().atMost(2, TimeUnit.SECONDS).until(
                () -> !wireMockRule.findAll(RequestPatternBuilder.newRequestPattern().withUrl("/v2/notifications/email")).isEmpty());
        
        wireMockRule.verify(1, postRequestedFor(urlPathEqualTo( "/v2/notifications/email"))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withRequestBody(matchingJsonPath("$.email_address", equalTo(adminUserEmail)))
                .withRequestBody(matchingJsonPath("$.template_id", equalTo("pay-notify-stripe-dispute-won-email-template-id")))
                .withRequestBody(matchingJsonPath("$.email_reply_to_id", equalTo("pay-notify-email-reply-to-support-id")))
                .withRequestBody(matchingJsonPath("$.personalisation.serviceName", equalTo(serviceName)))
                .withRequestBody(matchingJsonPath("$.personalisation.organisationName", equalTo(organisationName)))
                
        );
    }

    public void setMessage(byte[] messageContents) {
        currentMessage = messageContents;
    }
}
