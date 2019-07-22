package uk.gov.pay.publicauth.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.db.DataSourceFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import static java.lang.String.format;

public class DatabaseMetricsService {

    private final Set<PostgresMetric> metrics = Set.of(
            new PostgresGaugeMetric("numbackends", 0L),
            new PostgresCounterMetric("xact_commit", 0L),
            new PostgresCounterMetric("xact_rollback", 0L),
            new PostgresCounterMetric("blks_read", 0L),
            new PostgresCounterMetric("blks_hit", 0L),
            new PostgresCounterMetric("tup_returned", 0L),
            new PostgresCounterMetric("tup_fetched", 0L),
            new PostgresCounterMetric("tup_inserted", 0L),
            new PostgresCounterMetric("tup_updated", 0L),
            new PostgresCounterMetric("tup_deleted", 0L),
            new PostgresCounterMetric("conflicts", 0L),
            new PostgresCounterMetric("temp_files", 0L),
            new PostgresCounterMetric("temp_bytes", 0L),
            new PostgresCounterMetric("deadlocks", 0L),
            new PostgresCounterMetric("blk_read_time_ns", 0L),
            new PostgresCounterMetric("blk_write_time_ns", 0L));

    private final String databaseName;
    private final DataSourceFactory dataSourceFactory;
    private Integer statsHealthy = 0;

    public DatabaseMetricsService(DataSourceFactory dataSourceFactory, MetricRegistry metricRegistry, String databaseName) {
        this.dataSourceFactory = dataSourceFactory;
        this.databaseName = databaseName;
        metrics.forEach(metric -> metric.register(metricRegistry, format("%sdb.", databaseName)));
        metricRegistry.<Gauge<Integer>>register(format("%sdb.stats_healthy", databaseName), () -> statsHealthy);
    }

    public void updateMetricData() {
        statsHealthy = fetchDatabaseMetrics() ? 1 : 0;
    }

    private boolean fetchDatabaseMetrics() {
        try (Connection connection = DriverManager.getConnection(
                dataSourceFactory.getUrl(),
                dataSourceFactory.getUser(),
                dataSourceFactory.getPassword());
             PreparedStatement statement = connection.prepareStatement("select *, blk_read_time * 1000 as blk_read_time_ns, blk_write_time * 1000 as blk_write_time_ns from pg_stat_database where datname = ?")) {
            connection.setReadOnly(true);
            statement.setString(1, databaseName);
            if (statement.execute()) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    if (resultSet.next()) {
                        for (PostgresMetric metric : metrics) {
                            metric.setValue(resultSet.getLong(metric.getName()));
                        }
                        return true;
                    }
                }
            }
        } catch (SQLException ignored) {
        }

        return false;
    }

    private interface PostgresMetric {
        String getName();
        void register(MetricRegistry registry, String prefix);
        void setValue(Long value);
    }

    private class PostgresGaugeMetric implements PostgresMetric,Gauge<Long> {
        final String name;
        Long value;

        PostgresGaugeMetric(String name, Long defaultValue) {
            this.name = name;
            this.value = defaultValue;
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
            registry.<Gauge<Long>>register(format("%s%s", prefix, name), this);
        }

        public void setValue(Long value) {
            this.value = value;
        }
    }

    private class PostgresCounterMetric extends Counter implements PostgresMetric {
        final String name;
        Long value;

        PostgresCounterMetric(String name, Long defaultValue) {
            this.name = name;
            this.value = defaultValue;
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
            registry.<Counter>register(format("%s%s", prefix, name), this);
        }

        @Override
        public void setValue(Long value) {
            this.value = value;
        }
    }
}
