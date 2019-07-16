package uk.gov.pay.publicauth.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Environment;
import uk.gov.pay.publicauth.service.DatabaseMetricsService;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseHealthCheck extends HealthCheck {

    private final DataSourceFactory dataSourceFactory;
    private final DatabaseMetricsService metricsService;

    public DatabaseHealthCheck(DataSourceFactory dataSourceFactory, Environment environment, String databaseName) {
        this.dataSourceFactory = dataSourceFactory;
        this.metricsService = new DatabaseMetricsService(dataSourceFactory, environment.metrics(), databaseName);
    }

    @Override
    protected Result check() {
        try (Connection connection = DriverManager.getConnection(
                dataSourceFactory.getUrl(),
                dataSourceFactory.getUser(),
                dataSourceFactory.getPassword())) {
            connection.setReadOnly(true);
            metricsService.updateMetricData();

            return connection.isValid(2) ? Result.healthy() : Result.unhealthy("Could not validate the DB connection.");
        } catch (Exception e) {
            return Result.unhealthy(e.getMessage());
        }
    }
}
