package uk.gov.pay.adminusers.infra;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import uk.gov.pay.adminusers.app.AdminUsersApp;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs the adminusers application on random ports against a fresh database of its own on the local PostgreSQL server
 * (see {@link PostgresTestDatabase}), with the settings of the integration tests: ledger is expected on
 * {@link #getWireMockPort()} unless overridden, the SQS queues on the {@link SqsTestServer}. The application starts
 * before the tests of a class and stops after them, so test classes that share an instance each get an application
 * and a database of their own.
 */
public class AppWithPostgresExtension implements BeforeAllCallback, AfterAllCallback {

    private final String[] configOverrides;
    private final int wireMockPort = findFreePort();

    private SqsTestServer sqsTestServer;
    private PostgresTestDatabase database;
    private ConfigurableApplicationContext applicationContext;
    private DatabaseTestHelper databaseTestHelper;

    /**
     * @param configOverrides settings that replace those of the integration tests, as pairs of Spring property name
     *                        and value
     */
    public AppWithPostgresExtension(String... configOverrides) {
        this.configOverrides = configOverrides;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (context.getRequiredTestClass().getEnclosingClass() != null) {
            // a @Nested class runs with the application of its enclosing class
            return;
        }
        sqsTestServer = SqsTestServer.start(Collections.singletonList("event-queue"));
        database = PostgresTestDatabase.create();
        database.migrateToInitialDbStateAndChangeLog();
        databaseTestHelper = database.createDatabaseTestHelper();

        Map<String, String> settings = testSettings(database);
        settings.put("adminusers.sqs-config.connector-tasks-queue-url", sqsTestServer.getQueueUrl("event-queue"));
        settings.put("adminusers.sqs-config.endpoint", sqsTestServer.getEndpoint());
        settings.put("adminusers.ledger-base-url", "http://localhost:" + wireMockPort);
        for (int i = 0; i + 1 < configOverrides.length; i += 2) {
            settings.put(configOverrides[i], configOverrides[i + 1]);
        }
        applicationContext = start(settings);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (context.getRequiredTestClass().getEnclosingClass() == null) {
            applicationContext.close();
            database.drop();
            sqsTestServer.close();
        }
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

    public int getWireMockPort() {
        return wireMockPort;
    }

    /**
     * The settings the integration tests run the application with, as their configuration file had them: the defaults
     * of the service, with the SQS queues on a non-standard endpoint, the expunge and archive task enabled and short
     * queue delays.
     */
    public static Map<String, String> testSettings(PostgresTestDatabase database) {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("server.port", "0");
        settings.put("management.server.port", "0");
        settings.put("logging.level.root", "ERROR");
        settings.put("spring.datasource.url", database.getJdbcUrl());
        settings.put("spring.datasource.username", database.getUsername());
        settings.put("spring.datasource.password", database.getPassword());
        settings.put("adminusers.environment", "test");
        settings.put("adminusers.sqs-config.non-standard-service-endpoint", "true");
        settings.put("adminusers.sqs-config.endpoint", "http://localhost");
        settings.put("adminusers.sqs-config.region", "region-1");
        settings.put("adminusers.sqs-config.secret-key", "x");
        settings.put("adminusers.sqs-config.access-key", "x");
        settings.put("adminusers.sqs-config.event-subscriber-queue-url", "");
        settings.put("adminusers.sqs-config.connector-tasks-queue-url", "");
        settings.put("adminusers.event-subscriber-queue-config.queue-scheduler-thread-delay-in-milliseconds", "1");
        settings.put("adminusers.expunge-and-archive-data-config.expunge-and-archive-historical-data-enabled", "true");
        settings.put("adminusers.connector-task-queue-config.failed-message-retry-delay-in-seconds", "1");
        settings.put("adminusers.ledger-base-url", "");
        return settings;
    }

    static ConfigurableApplicationContext start(Map<String, String> settings) {
        return new SpringApplicationBuilder(AdminUsersApp.class).run(toArguments(settings));
    }

    public static String[] toArguments(Map<String, String> settings) {
        return settings.entrySet().stream()
                .map(setting -> "--" + setting.getKey() + "=" + setting.getValue())
                .toArray(String[]::new);
    }

    public static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
