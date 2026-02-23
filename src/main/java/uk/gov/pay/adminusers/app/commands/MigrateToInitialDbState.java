package uk.gov.pay.adminusers.app.commands;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Import;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The {@code migrateToInitialDbState} command: migrates to the initial (selfservice) database state, if necessary.
 */
@Import(CommandConfiguration.class)
public class MigrateToInitialDbState implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateToInitialDbState.class);

    private final DataSourceProperties dataSourceProperties;

    public MigrateToInitialDbState(DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = DatabaseConnections.open(dataSourceProperties);
             PreparedStatement statement = connection.prepareStatement("select exists (select * from pg_tables where tablename='users')");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                boolean usersExists = resultSet.getBoolean(1);
                if (usersExists) {
                    LOGGER.info("Users table found in current environment. Not required for the initial database migration");
                } else {
                    LOGGER.info("Users table not found. Preparing for the initial database migration..");
                    performInitialMigration(connection);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error during initial DB setup", e);
            throw new RuntimeException(e);
        }
    }

    private void performInitialMigration(Connection connection) {
        try {
            Liquibase migrator = new Liquibase("config/initial-db-state.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator.update("");
        } catch (LiquibaseException e) {
            LOGGER.error("Error performing liquibase initial database migration", e);
            throw new RuntimeException(e);
        }
    }
}
