package uk.gov.pay.adminusers.app.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static java.lang.String.format;

/**
 * Publishes the PostgreSQL statistics of a database ({@code pg_stat_database}) as metrics named
 * {@code <database>db.<statistic>}, with {@code <database>db.stats_healthy} telling whether the last update worked.
 */
public class DatabaseMetricsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMetricsService.class);

    private static final String QUERY = "select *, blk_read_time * 1000 as blk_read_time_ns, " +
            "blk_write_time * 1000 as blk_write_time_ns from pg_stat_database where datname = ?";

    private final List<PostgresMetric> metrics = List.of(
            new PostgresGauge("numbackends"),
            new PostgresCounter("xact_commit"),
            new PostgresCounter("xact_rollback"),
            new PostgresCounter("blks_read"),
            new PostgresCounter("blks_hit"),
            new PostgresCounter("tup_returned"),
            new PostgresCounter("tup_fetched"),
            new PostgresCounter("tup_inserted"),
            new PostgresCounter("tup_updated"),
            new PostgresCounter("tup_deleted"),
            new PostgresCounter("conflicts"),
            new PostgresCounter("temp_files"),
            new PostgresCounter("temp_bytes"),
            new PostgresCounter("deadlocks"),
            new PostgresCounter("blk_read_time_ns"),
            new PostgresCounter("blk_write_time_ns"));

    private final DataSourceProperties dataSourceProperties;
    private final String databaseName;
    private volatile int statsHealthy = 0;

    public DatabaseMetricsService(DataSourceProperties dataSourceProperties, MetricRegistry metricRegistry,
                                  String databaseName) {
        this.dataSourceProperties = dataSourceProperties;
        this.databaseName = databaseName;
        metrics.forEach(metric -> metric.register(metricRegistry, format("%sdb.", databaseName)));
        metricRegistry.<Gauge<Integer>>register(format("%sdb.stats_healthy", databaseName), () -> statsHealthy);
    }

    public void updateMetricData() {
        statsHealthy = fetchDatabaseMetrics() ? 1 : 0;
    }

    private boolean fetchDatabaseMetrics() {
        try (Connection connection = DriverManager.getConnection(dataSourceProperties.determineUrl(),
                dataSourceProperties.determineUsername(), dataSourceProperties.determinePassword());
             PreparedStatement statement = connection.prepareStatement(QUERY)) {
            connection.setReadOnly(true);
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    for (PostgresMetric metric : metrics) {
                        metric.setValue(resultSet.getLong(metric.getName()));
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            LOGGER.warn("Unable to collect the database statistics: {}", e.getMessage(), e);
            return false;
        }
        return false;
    }

    private interface PostgresMetric {

        String getName();

        void register(MetricRegistry registry, String prefix);

        void setValue(long value);
    }

    private static final class PostgresGauge implements PostgresMetric, Gauge<Long> {

        private final String name;
        private volatile long value;

        private PostgresGauge(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Long getValue() {
            return value;
        }

        @Override
        public void register(MetricRegistry registry, String prefix) {
            registry.register(prefix + name, this);
        }

        @Override
        public void setValue(long value) {
            this.value = value;
        }
    }

    private static final class PostgresCounter extends Counter implements PostgresMetric {

        private final String name;
        private volatile long value;

        private PostgresCounter(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getCount() {
            return value;
        }

        @Override
        public void register(MetricRegistry registry, String prefix) {
            registry.register(prefix + name, this);
        }

        @Override
        public void setValue(long value) {
            this.value = value;
        }
    }
}
