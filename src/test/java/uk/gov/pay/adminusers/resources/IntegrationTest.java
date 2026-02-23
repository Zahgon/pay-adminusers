package uk.gov.pay.adminusers.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.adminusers.infra.AppWithPostgresExtension;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.pay.adminusers.infra.AppWithPostgresExtension.findFreePort;

@TestInstance(PER_CLASS)
public class IntegrationTest {
    /* default */ static final String USERS_RESOURCE_URL = "/v1/api/users";
    /* default */ static final String FIND_RESOURCE_URL = "/v1/api/users/find";
    /* default */ static final String USER_RESOURCE_URL = "/v1/api/users/%s";
    /* default */ static final String USERS_AUTHENTICATE_URL = "/v1/api/users/authenticate";
    /* default */ static final String USER_2FA_URL = "/v1/api/users/%s/second-factor";
    /* default */ static final String USER_SERVICE_RESOURCE = "/v1/api/users/%s/services/%s";

    /* default */ static final String INVITES_RESOURCE_URL = "/v1/api/invites";
    /* default */ static final String INVITES_SEND_OTP_RESOURCE_URL = "/v1/api/invites/%s/send-otp";
    /* default */ static final String INVITES_REPROVISION_OTP_RESOURCE_URL = "/v1/api/invites/%s/reprovision-otp";
    /* default */ static final String INVITES_VALIDATE_OTP_RESOURCE_URL = "/v2/api/invites/otp/validate";
    /* default */ static final String SERVICES_RESOURCE = "/v1/api/services";
    /* default */ static final String SERVICE_RESOURCE = SERVICES_RESOURCE + "/%s";

    @Deprecated
    /* default */ static final String SERVICE_INVITES_RESOURCE_URL = "/v1/api/services/%d/invites";

    private static final int NOTIFY_PORT = findFreePort();

    @RegisterExtension
    public static final AppWithPostgresExtension APP = new AppWithPostgresExtension(
            "adminusers.notify-configuration.notification-base-url", "http://localhost:" + NOTIFY_PORT);

    private WireMockServer notifyServer;
    protected DatabaseTestHelper databaseHelper;
    protected static ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    public void startNotify() {
        notifyServer = new WireMockServer(wireMockConfig().port(NOTIFY_PORT).bindAddress("localhost"));
        notifyServer.start();
        String response = "{\"id\":\"f1356064-37b6-499c-bec9-a167646255ff\", \"content\": {\"subject\":\"hello\", \"body\":\"bla\"}, \"template\": {\"id\":\"f1356064-37b6-499c-bec9-a167646255ff\", \"version\":0, \"uri\":\"lol\"}}";
        notifyServer.stubFor(post(urlPathEqualTo("/v2/notifications/email"))
                .withHeader(CONTENT_TYPE, containing(APPLICATION_JSON_VALUE))
                .willReturn(aResponse().withStatus(201).withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE).withBody(response)));
    }

    @AfterAll
    public void stopNotify() {
        notifyServer.stop();
    }

    @BeforeEach
    public void initialise() {
        databaseHelper = APP.getDatabaseTestHelper();
        databaseHelper.truncateAllData();
    }

    protected RequestSpecification givenSetup() {
        return given().port(APP.getLocalPort())
                .contentType(JSON);
    }

    protected static <T> T getBean(Class<T> beanClass) {
        return APP.getBean(beanClass);
    }
}
