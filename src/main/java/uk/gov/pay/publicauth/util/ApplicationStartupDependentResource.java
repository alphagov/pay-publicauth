package uk.gov.pay.publicauth.util;

import uk.gov.pay.publicauth.app.PublicAuthConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ApplicationStartupDependentResource {

    private final PublicAuthConfiguration configuration;

    public ApplicationStartupDependentResource(PublicAuthConfiguration configuration) {
        this.configuration = configuration;
    }

    public Connection getDatabaseConnection() throws SQLException {
        return DriverManager.getConnection(
                configuration.getDataSourceFactory().getUrl(),
                configuration.getDataSourceFactory().getUser(),
                configuration.getDataSourceFactory().getPassword());
    }

    public void sleep(long durationSeconds) {
        try {
            Thread.sleep(durationSeconds);
        } catch (InterruptedException ignored) {}
    }

}
