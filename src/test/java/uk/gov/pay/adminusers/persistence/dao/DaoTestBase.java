package uk.gov.pay.adminusers.persistence.dao;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import uk.gov.pay.adminusers.app.config.AdminUsersConfig;
import uk.gov.pay.adminusers.app.config.PersistenceConfiguration;
import uk.gov.pay.adminusers.infra.PostgresTestDatabase;
import uk.gov.pay.adminusers.model.Permission;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomInt;
import static uk.gov.pay.adminusers.app.util.RandomIdGenerator.randomUuid;
import static uk.gov.pay.adminusers.infra.AppWithPostgresExtension.testSettings;
import static uk.gov.pay.adminusers.infra.AppWithPostgresExtension.toArguments;
import static uk.gov.pay.adminusers.model.Permission.permission;

public class DaoTestBase {

    private static PostgresTestDatabase database;

    protected static DatabaseTestHelper databaseHelper;
    protected static ConfigurableApplicationContext env;

    @BeforeAll
    public static void setup() {
        database = PostgresTestDatabase.create();
        databaseHelper = database.createDatabaseTestHelper();
        database.migrate("config/initial-db-state.xml");
        database.migrate("it-migrations.xml");

        env = new SpringApplicationBuilder(DataAccessConfiguration.class)
                .web(WebApplicationType.NONE)
                .run(toArguments(testSettings(database)));
    }

    @AfterAll
    public static void cleanUp() {
        env.close();
        database.drop();
    }

    protected Permission aPermission() {
        return permission(randomInt(), "permission-name-" + randomUuid(), "permission-description" + randomUuid());
    }

    /**
     * The service's data source and persistence unit with its DAOs, and nothing else. Not a {@code @Configuration},
     * so that the service's component scan, which covers the test classes too, leaves it out.
     */
    @ImportAutoConfiguration(DataSourceAutoConfiguration.class)
    @EnableConfigurationProperties(AdminUsersConfig.class)
    @Import({PersistenceConfiguration.class, UserDao.class, ForgottenPasswordDao.class, RoleDao.class, ServiceDao.class,
            ServiceRoleDao.class, InviteDao.class, StripeAgreementDao.class, GovUkPayAgreementDao.class})
    static class DataAccessConfiguration {
    }
}
