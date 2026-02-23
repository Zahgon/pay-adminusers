package uk.gov.pay.adminusers.app.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Import;

import java.sql.SQLException;
import java.time.Duration;

import static java.lang.String.format;

/**
 * The {@code waitOnDependencies} command: waits, five seconds longer each time, until the configured database
 * accepts connections.
 */
@Import(CommandConfiguration.class)
public class DependentResourceWaitCommand implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependentResourceWaitCommand.class);
    private static final int PROGRESSIVE_SECONDS_TO_WAIT = 5;

    private final DataSourceProperties dataSourceProperties;

    public DependentResourceWaitCommand(DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        String database = format("DatabaseStartupResource[url=%s, user=%s]",
                dataSourceProperties.determineUrl(), dataSourceProperties.determineUsername());
        long timeToWait = 0;
        while (!databaseAvailable()) {
            timeToWait += PROGRESSIVE_SECONDS_TO_WAIT;
            LOGGER.info("Waiting for {} seconds until {} is available ...", timeToWait, database);
            Thread.sleep(Duration.ofSeconds(timeToWait));
        }
        LOGGER.info("{} available.", database);
    }

    private boolean databaseAvailable() {
        try {
            DatabaseConnections.open(dataSourceProperties).close();
            return true;
        } catch (SQLException e) {
            LOGGER.warn("Unable to connect to database: {}", e.getMessage());
            return false;
        }
    }
}
