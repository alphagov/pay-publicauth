package uk.gov.pay.publicauth.util;


import io.dropwizard.db.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DriverManager;
import java.sql.SQLException;

import static java.lang.String.format;

public class DatabaseStartupResource implements ApplicationStartupDependentResource {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseStartupResource.class);

    private final String databaseUrl;
    private final String databaseUser;
    private final String databasePassword;

    DatabaseStartupResource(DataSourceFactory dataSourceFactory) {
        databaseUrl = dataSourceFactory.getUrl();
        databaseUser = dataSourceFactory.getUser();
        databasePassword = dataSourceFactory.getPassword();
    }

    public boolean isAvailable() {
        try {
            DriverManager.getConnection(databaseUrl, databaseUser, databasePassword).close();
            return true;
        } catch (SQLException e) {
            logger.warn("Unable to connect to database: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String toString() {
        return format("DatabaseStartupResource[url=%s, user=%s]", databaseUrl, databaseUser);
    }
}
