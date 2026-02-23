package uk.gov.pay.adminusers.app.commands;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens connections to the configured database for the commands that run outside the server.
 */
final class DatabaseConnections {

    private DatabaseConnections() {
    }

    static Connection open(DataSourceProperties dataSourceProperties) throws SQLException {
        return DriverManager.getConnection(dataSourceProperties.determineUrl(),
                dataSourceProperties.determineUsername(), dataSourceProperties.determinePassword());
    }
}
