package uk.gov.pay.adminusers.app.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Healthy when a new read-only connection to the service's database can be opened and validated.
 */
public class DatabaseHealthCheck extends HealthCheck {

    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSourceProperties dataSourceProperties;

    public DatabaseHealthCheck(DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    @Override
    protected Result check() {
        try (Connection connection = DriverManager.getConnection(dataSourceProperties.determineUrl(),
                dataSourceProperties.determineUsername(), dataSourceProperties.determinePassword())) {
            connection.setReadOnly(true);
            return connection.isValid(VALIDATION_TIMEOUT_SECONDS)
                    ? Result.healthy()
                    : Result.unhealthy("Could not validate the DB connection.");
        } catch (Exception e) {
            return Result.unhealthy(e);
        }
    }
}
