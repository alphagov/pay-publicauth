package uk.gov.pay.publicauth.util;

import com.google.common.base.Stopwatch;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.app.PublicAuthConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.TimeUnit;

public class DbWaitCommand extends ConfiguredCommand<PublicAuthConfiguration> {
    public static final String TIMEOUT_ARG = "timeout";
    private final Logger logger = LoggerFactory.getLogger(DbWaitCommand.class);

    public DbWaitCommand() {
        super("dbwait", "Waits for database to become available");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
        subparser
            .addArgument(TIMEOUT_ARG)
            .type(Integer.class)
            .help("how long to wait for the database to become available")
        ;
    }

    @Override
    protected void run(Bootstrap<PublicAuthConfiguration> bs, Namespace ns, PublicAuthConfiguration conf) {
        final int timeout = ns.getInt(TIMEOUT_ARG);
        final Stopwatch timer = Stopwatch.createStarted();

        logger.info("Waiting for database (max " + timeout + " seconds)");

        boolean succeeded = false;
        while (!succeeded && timer.elapsed(TimeUnit.SECONDS) < timeout) {
            sleep(500);
            succeeded = checkConnection(conf.getDataSourceFactory());
        }

        if (!succeeded) {
            throw new RuntimeException("Database was not available for " + timeout + " seconds");
        }
    }

    private boolean checkConnection(DataSourceFactory ds) {
        try (Connection conn = DriverManager.getConnection(ds.getUrl(), ds.getUser(), ds.getPassword())) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
        }
    }
}
