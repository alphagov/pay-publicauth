package uk.gov.pay.publicauth.utils;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.jdbi.v3.core.Jdbi;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.commons.testing.db.PostgresDockerRule;
import uk.gov.pay.publicauth.app.PublicAuthApp;
import uk.gov.pay.publicauth.app.config.PublicAuthConfiguration;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

public class DropwizardAppWithPostgresRule implements TestRule {

    private static final Logger logger = LoggerFactory.getLogger(DropwizardAppWithPostgresRule.class);

    private final String configFilePath = resourceFilePath("config/test-it-config.yaml");

    private final PostgresDockerRule postgres = new PostgresDockerRule();

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
                app.getApplication().run("db", "migrate", configFilePath);

                databaseHelper = new DatabaseTestHelper(getJdbi());

                restoreDropwizardsLogging();

                base.evaluate();
            }
        }, description);
    }

    public Jdbi getJdbi() {
        return app.<PublicAuthApp>getApplication().getJdbi();
    }

    public int getLocalPort() {
        return app.getLocalPort();
    }

    public void stopPostgres() {
        postgres.stop();
    }

    private void restoreDropwizardsLogging() {
        app.getConfiguration().getLoggingFactory().configure(app.getEnvironment().metrics(),
                app.getApplication().getName());
    }

    public DatabaseTestHelper getDatabaseHelper() {
        return databaseHelper;
    }
}
