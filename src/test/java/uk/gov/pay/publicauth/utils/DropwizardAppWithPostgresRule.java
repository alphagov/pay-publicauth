package uk.gov.pay.publicauth.utils;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.pay.publicauth.app.PublicAuthApp;
import uk.gov.pay.publicauth.app.config.PublicAuthConfiguration;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

public class DropwizardAppWithPostgresRule implements TestRule {

    private static final Logger logger = LoggerFactory.getLogger(DropwizardAppWithPostgresRule.class);

    private final String configFilePath = resourceFilePath("config/test-it-config.yaml");

    private final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:9.6.6");

    private final DropwizardAppRule<PublicAuthConfiguration> app;

    private final RuleChain rules;
    private DatabaseTestHelper databaseHelper;

    public DropwizardAppWithPostgresRule() {
        postgreSQLContainer.start();
        app = new DropwizardAppRule<>(
                PublicAuthApp.class,
                configFilePath,
                config("database.url", postgreSQLContainer.getJdbcUrl()),
                config("database.user", postgreSQLContainer.getUsername()),
                config("database.password", postgreSQLContainer.getPassword()));

        rules = RuleChain.outerRule(postgreSQLContainer).around(app);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return rules.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                logger.info("Clearing database.");
                app.getApplication().run("db", "drop-all", "--confirm-delete-everything", configFilePath);
                app.getApplication().run("db", "migrate", configFilePath);

                databaseHelper = new DatabaseTestHelper(getJdbi());

                restoreDropwizardsLogging();


                base.evaluate();
            }
        }, description);
    }

    public DBI getJdbi() {
        return app.<PublicAuthApp>getApplication().getJdbi();
    }

    public int getLocalPort() {
        return app.getLocalPort();
    }

    public void stopPostgres() {
        postgreSQLContainer.stop();
    }

    private void restoreDropwizardsLogging() {
        app.getConfiguration().getLoggingFactory().configure(app.getEnvironment().metrics(),
                app.getApplication().getName());
    }

    public DatabaseTestHelper getDatabaseHelper() {
        return databaseHelper;
    }
}
