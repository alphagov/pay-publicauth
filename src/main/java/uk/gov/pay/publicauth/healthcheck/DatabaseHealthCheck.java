package uk.gov.pay.publicauth.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.db.DataSourceFactory;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseHealthCheck extends HealthCheck {

    private final DataSourceFactory dataSourceFactory;

    public DatabaseHealthCheck(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    protected Result check() {
        try (Connection connection = DriverManager.getConnection(
                dataSourceFactory.getUrl(),
                dataSourceFactory.getUser(),
                dataSourceFactory.getPassword())) {
            connection.setReadOnly(true);

            return connection.isValid(2) ? Result.healthy() : Result.unhealthy("Could not validate the DB connection.");
        } catch (Exception e) {
            return Result.unhealthy(e.getMessage());
        }
    }
}
