package uk.gov.pay.publicauth.util;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class DbConnectionChecker {
    public static final int POSTGRES_STARTUP_TIMEOUT = 30;
    private final Logger logger = LoggerFactory.getLogger(DbConnectionChecker.class);

    private final String dbUrl;
    private final String user;
    private final String password;

    public DbConnectionChecker(String dbUrl, String user, String password) {
        this.dbUrl = dbUrl;
        this.user = user;
        this.password = password;
    }

    public void waitForPostgresToStart() {
        Stopwatch timer = Stopwatch.createStarted();
        boolean succeeded = false;
        while (!succeeded && timer.elapsed(TimeUnit.SECONDS) < POSTGRES_STARTUP_TIMEOUT) {
            sleep(10);
            succeeded = checkPostgresConnection();
        }
        if (!succeeded) {
            throw new RuntimeException(format("Postgres did not start in %d seconds.", POSTGRES_STARTUP_TIMEOUT));
        }
        logger.info("Postgres docker container started in {}", timer.elapsed(TimeUnit.MILLISECONDS));
    }

    private boolean checkPostgresConnection() {
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);

        try (Connection connection = DriverManager.getConnection(dbUrl, props)) {
            return true;
        } catch (Exception except) {
            return false;
        }
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
