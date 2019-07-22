package uk.gov.pay.publicauth.service;

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
            new LongPostgresMetric("numbackends", 0L),
            new LongPostgresMetric("xact_commit", 0L),
            new LongPostgresMetric("xact_rollback", 0L),
            new LongPostgresMetric("blks_read", 0L),
            new LongPostgresMetric("blks_hit", 0L),
            new LongPostgresMetric("tup_returned", 0L),
            new LongPostgresMetric("tup_fetched", 0L),
            new LongPostgresMetric("tup_inserted", 0L),
            new LongPostgresMetric("tup_updated", 0L),
            new LongPostgresMetric("tup_deleted", 0L),
            new LongPostgresMetric("conflicts", 0L),
            new LongPostgresMetric("temp_files", 0L),
            new LongPostgresMetric("temp_bytes", 0L),
            new LongPostgresMetric("deadlocks", 0L),
            new DoublePostgresMetric("blk_read_time", 0.0),
            new DoublePostgresMetric("blk_write_time", 0.0));

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
             PreparedStatement statement = connection.prepareStatement("select * from pg_stat_database where datname = ?")) {
            connection.setReadOnly(true);
            statement.setString(1, databaseName);
            if (statement.execute()) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    if (resultSet.next()) {
                        for (PostgresMetric metric : metrics) {
                            metric.setValueFromResultSet(resultSet);
                        }
                        return true;
                    }
                }
            }
        } catch (SQLException ignored) {
        }

        return false;
    }

    private abstract class PostgresMetric<T> implements Gauge<T> {
        private final String name;
        T value;

        PostgresMetric(String name, T defaultValue) {
            this.name = name;
            this.value = defaultValue;
        }

        String getName() {
            return name;
        }

        @Override
        public T getValue() {
            return value;
        }

        void register(MetricRegistry registry, String prefix) {
            registry.<Gauge<T>>register(format("%s%s", prefix, name), this);
        }

        abstract void setValueFromResultSet(ResultSet resultSet) throws SQLException;
    }

    private class DoublePostgresMetric extends PostgresMetric<Double> {
        DoublePostgresMetric(String name, Double value) {
            super(name, value);
        }

        @Override
        void setValueFromResultSet(ResultSet resultSet) throws SQLException {
            value = resultSet.getDouble(getName());
        }
    }

    private class LongPostgresMetric extends PostgresMetric<Long> {
        LongPostgresMetric(String name, Long value) {
            super(name, value);
        }

        @Override
        void setValueFromResultSet(ResultSet resultSet) throws SQLException {
            value = resultSet.getLong(getName());
        }
    }
}
