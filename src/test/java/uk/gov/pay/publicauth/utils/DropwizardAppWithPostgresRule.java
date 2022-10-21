package uk.gov.pay.publicauth.utils;

import io.dropwizard.testing.junit.DropwizardAppRule;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.service.payments.commons.testing.db.PostgresDockerRule;
import uk.gov.pay.publicauth.app.PublicAuthApp;
import uk.gov.pay.publicauth.app.config.PublicAuthConfiguration;
import uk.gov.service.payments.commons.testing.db.PostgresTestHelper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static uk.gov.pay.publicauth.utils.PostgresTestContainersBase.VERSION;

public class DropwizardAppWithPostgresRule implements TestRule {

    private static final Logger logger = LoggerFactory.getLogger(DropwizardAppWithPostgresRule.class);

    private final String configFilePath = resourceFilePath("config/test-it-config.yaml");

    private final PostgresDockerRule postgres = new PostgresDockerRule(VERSION);

    private final DropwizardAppRule<PublicAuthConfiguration> app = new DropwizardAppRule<>(
            PublicAuthApp.class,
            configFilePath,
            config("database.url", postgres.getConnectionUrl()),
            config("database.user", postgres.getUsername()),
            config("database.password", postgres.getPassword()));

    private final RuleChain rules = RuleChain.outerRule(postgres).around(app);
    private DatabaseTestHelper databaseHelper;

    @Override
    public Statement apply(Statement base, Description description) {
        return rules.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                logger.info("Clearing database.");
                app.getApplication().run("db", "drop-all", "--confirm-delete-everything", configFilePath);
                doMigration();
                databaseHelper = new DatabaseTestHelper(getJdbi());

                restoreDropwizardsLogging();

                base.evaluate();
            }
        }, description);
    }

    private void doMigration() throws SQLException, LiquibaseException {
        try (Connection connection = DriverManager.getConnection(postgres.getConnectionUrl(), postgres.getUsername(), postgres.getPassword())) {
            logger.info("Running migrations.");
            Liquibase migrator = new Liquibase("it-migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator.update("");
        }
    }

    public Jdbi getJdbi() {
        return app.<PublicAuthApp>getApplication().getJdbi();
    }

    public int getLocalPort() {
        return app.getLocalPort();
    }

    public void stopPostgres() {
        PostgresTestHelper.stop();
    }

    private void restoreDropwizardsLogging() {
        app.getConfiguration().getLoggingFactory().configure(app.getEnvironment().metrics(),
                app.getApplication().getName());
    }

    public DatabaseTestHelper getDatabaseHelper() {
        return databaseHelper;
    }
}
