package uk.gov.pay.adminusers.app.commands;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringResourceAccessor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;

import java.sql.Connection;

/**
 * The {@code db migrate} command: applies all pending change sets of {@code migrations.xml} (which includes every
 * change log in the {@code migrations} directory of the class path) to the configured database.
 */
@Import(CommandConfiguration.class)
public class DatabaseMigrationCommand implements ApplicationRunner {

    private static final String MIGRATIONS_FILE = "migrations.xml";

    private final DataSourceProperties dataSourceProperties;
    private final ResourceLoader resourceLoader;

    public DatabaseMigrationCommand(DataSourceProperties dataSourceProperties, ResourceLoader resourceLoader) {
        this.dataSourceProperties = dataSourceProperties;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = DatabaseConnections.open(dataSourceProperties)) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase(MIGRATIONS_FILE, new SpringResourceAccessor(resourceLoader), database)) {
                liquibase.update("");
            }
        }
    }
}
