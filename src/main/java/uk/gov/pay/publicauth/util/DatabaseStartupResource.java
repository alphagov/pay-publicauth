package uk.gov.pay.publicauth.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.app.config.PublicAuthConfiguration;

import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseStartupResource implements ApplicationStartupDependentResource {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseStartupResource.class);

    private final PublicAuthConfiguration configuration;

    DatabaseStartupResource(PublicAuthConfiguration configuration) {
        this.configuration = configuration;
    }

    public boolean isAvailable() {
        try {
            DriverManager.getConnection(
                    configuration.getDataSourceFactory().getUrl(),
                    configuration.getDataSourceFactory().getUser(),
                    configuration.getDataSourceFactory().getPassword()).close();
            return true;
        } catch (SQLException e) {
            logger.warn("Unable to connect to database: {}", e.getMessage());
            return false;
        }
    }
}
