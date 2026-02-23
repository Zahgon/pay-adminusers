package uk.gov.pay.adminusers.infra;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import uk.gov.pay.adminusers.app.commands.MigrateToInitialDbState;
import uk.gov.pay.adminusers.utils.DatabaseTestHelper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * A fresh database on a local PostgreSQL server. The server is found through the system properties
 * {@code postgres.host}, {@code postgres.port}, {@code postgres.user} and {@code postgres.password}, or else the
 * environment variables {@code POSTGRES_HOST}, {@code POSTGRES_PORT}, {@code POSTGRES_USER} and
 * {@code POSTGRES_PASSWORD}; they default to {@code localhost}, {@code 5432}, {@code postgres} and {@code postgres}.
 * Every instance gets a database of its own, which {@link #drop()} removes.
 */
public class PostgresTestDatabase {

    private static final String DATABASE_NAME_PREFIX = "t_adminusers_";
    private static final AtomicInteger DATABASE_COUNTER = new AtomicInteger();

    private final String host = setting("postgres.host", "POSTGRES_HOST", "localhost");
    private final String port = setting("postgres.port", "POSTGRES_PORT", "5432");
    private final String username = setting("postgres.user", "POSTGRES_USER", "postgres");
    private final String password = setting("postgres.password", "POSTGRES_PASSWORD", "postgres");
    private final String databaseName = DATABASE_NAME_PREFIX + ProcessHandle.current().pid() + "_" + DATABASE_COUNTER.incrementAndGet();

    private PostgresTestDatabase() {
    }

    /**
     * Creates a new, empty database.
     */
    public static PostgresTestDatabase create() {
        PostgresTestDatabase database = new PostgresTestDatabase();
        database.drop();
        database.executeOnServer(format("CREATE DATABASE %s", database.databaseName));
        return database;
    }

    public String getJdbcUrl() {
        return jdbcUrl(databaseName);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public DatabaseTestHelper createDatabaseTestHelper() {
        return new DatabaseTestHelper(Jdbi.create(getJdbcUrl(), username, password));
    }

    /**
     * Migrates the database as the service is migrated: to the initial (selfservice) database state of the
     * {@code migrateToInitialDbState} command, then with {@code it-migrations.xml} (the extensions the tests need and
     * the service's change log).
     */
    public void migrateToInitialDbStateAndChangeLog() {
        DataSourceProperties dataSourceProperties = new DataSourceProperties();
        dataSourceProperties.setUrl(getJdbcUrl());
        dataSourceProperties.setUsername(username);
        dataSourceProperties.setPassword(password);
        new MigrateToInitialDbState(dataSourceProperties).run(null);
        migrate("it-migrations.xml");
    }

    /**
     * Applies the given Liquibase change log of the class path.
     */
    public void migrate(String changeLog) {
        try (Connection connection = connect();
             Liquibase migrator = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), new JdbcConnection(connection))) {
            migrator.update("");
        } catch (SQLException | LiquibaseException e) {
            throw new IllegalStateException(format("Could not migrate the test database %s with %s", databaseName, changeLog), e);
        }
    }

    public void drop() {
        executeOnServer(format("DROP DATABASE IF EXISTS %s WITH (FORCE)", databaseName));
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(getJdbcUrl(), username, password);
    }

    private void executeOnServer(String sql) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl("postgres"), username, password);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(format("Could not run [%s] on PostgreSQL at %s:%s", sql, host, port), e);
        }
    }

    private String jdbcUrl(String database) {
        return format("jdbc:postgresql://%s:%s/%s", host, port, database);
    }

    private static String setting(String systemProperty, String environmentVariable, String defaultValue) {
        return Optional.ofNullable(System.getProperty(systemProperty))
                .or(() -> Optional.ofNullable(System.getenv(environmentVariable)))
                .orElse(defaultValue);
    }
}
