package uk.gov.pay.publicauth.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class ApplicationStartupDependentResourceChecker {

    private static final int PROGRESSIVE_SECONDS_TO_WAIT = 5;
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupDependentResourceChecker.class);

    private final ApplicationStartupDependentResource applicationStartupDependentResource;

    public ApplicationStartupDependentResourceChecker(ApplicationStartupDependentResource applicationStartupDependentResource) {
        this.applicationStartupDependentResource = applicationStartupDependentResource;
    }

    public void checkAndWaitForResources() {
        waitingForDatabaseConnectivity();
    }

    private void waitingForDatabaseConnectivity() {
        logger.info("Checking for database availability >>>");

        long timeToWait = 0;
        while(!isDatabaseAvailable()) {
            timeToWait += PROGRESSIVE_SECONDS_TO_WAIT;
            logger.info("Waiting for {} seconds till the database is available ...", timeToWait);
            applicationStartupDependentResource.sleep(timeToWait * 1000);
        }
        logger.info("Database available.");
    }


    private boolean isDatabaseAvailable() {
        try {
            applicationStartupDependentResource.getDatabaseConnection().close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}