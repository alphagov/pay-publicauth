package uk.gov.pay.publicauth.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Consumer;

public class ApplicationStartupDependentResourceChecker {

    private static final int PROGRESSIVE_SECONDS_TO_WAIT = 5;
    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupDependentResourceChecker.class);

    private final ApplicationStartupDependentResource applicationStartupDependentResource;
    private final Consumer<Duration> waiter;

    public ApplicationStartupDependentResourceChecker(ApplicationStartupDependentResource applicationStartupDependentResource, Consumer<Duration> waiter) {
        this.applicationStartupDependentResource = applicationStartupDependentResource;
        this.waiter = waiter;
    }

    public void checkAndWaitForResource() {
        long timeToWait = 0;
        while(!applicationStartupDependentResource.isAvailable()) {
            timeToWait += PROGRESSIVE_SECONDS_TO_WAIT;
            logger.info("Waiting for {} seconds till the database is available ...", timeToWait);
            waiter.accept(Duration.ofSeconds(timeToWait));
        }
        logger.info("Database available.");
    }

}
