package uk.gov.pay.adminusers.infra;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.util.Collections;
import java.util.Map;

import static uk.gov.pay.adminusers.infra.AppWithPostgresExtension.findFreePort;

/**
 * Runs the adminusers application on random ports against a fresh database of its own on the local PostgreSQL server
 * (see {@link PostgresTestDatabase}), with the SQS queues on the {@link SqsTestServer} and Notify and ledger expected
 * on {@link #getWireMockPort()}. Registered on a static field, the application runs for the tests of the class;
 * registered on an instance field, it runs for each test.
 */
public class AppWithPostgresAndSqsExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private final String[] configOverrides;
    private final int wireMockPort = findFreePort();

    private SqsTestServer sqsTestServer;
    private SqsClient sqsClient;
    private PostgresTestDatabase database;
    private DatabaseTestHelper databaseTestHelper;
    private ConfigurableApplicationContext applicationContext;
    private boolean startedForClass;

    /**
     * @param configOverrides settings that replace those of the integration tests, as pairs of Spring property name
     *                        and value
     */
    public AppWithPostgresAndSqsExtension(String... configOverrides) {
        this.configOverrides = configOverrides;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        startedForClass = true;
        start();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (!startedForClass) {
            start();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (!startedForClass) {
            stop();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (startedForClass) {
            stop();
        }
    }

    private void start() {
        sqsTestServer = SqsTestServer.start(Collections.singletonList("event-queue"));
        sqsClient = sqsTestServer.createSqsClient();
        database = PostgresTestDatabase.create();
        database.migrateToInitialDbStateAndChangeLog();
        databaseTestHelper = database.createDatabaseTestHelper();

        Map<String, String> settings = AppWithPostgresExtension.testSettings(database);
        settings.put("adminusers.sqs-config.event-subscriber-queue-url", sqsTestServer.getQueueUrl("event-queue"));
        settings.put("adminusers.sqs-config.endpoint", sqsTestServer.getEndpoint());
        settings.put("adminusers.notify-configuration.notification-base-url", "http://localhost:" + wireMockPort);
        settings.put("adminusers.ledger-base-url", "http://localhost:" + wireMockPort);
        for (int i = 0; i + 1 < configOverrides.length; i += 2) {
            settings.put(configOverrides[i], configOverrides[i + 1]);
        }
        applicationContext = AppWithPostgresExtension.start(settings);
    }

    private void stop() {
        applicationContext.close();
        database.drop();
        sqsClient.close();
        sqsTestServer.close();
    }

    public <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }

    public int getLocalPort() {
        return ((WebServerApplicationContext) applicationContext).getWebServer().getPort();
    }

    public DatabaseTestHelper getDatabaseTestHelper() {
        return databaseTestHelper;
    }

    public SqsClient getSqsClient() {
        return sqsClient;
    }

    public String getQueueUrl(String queueName) {
        return sqsTestServer.getQueueUrl(queueName);
    }

    public int getWireMockPort() {
        return wireMockPort;
    }

}
