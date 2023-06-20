package uk.gov.pay.publicauth.utils;

import io.dropwizard.testing.DropwizardTestSupport;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(DropwizardAppWithPostgresRule.class);
    private static final String CONFIG_FILE_PATH = resourceFilePath("config/test-it-config.yaml");
    private static final PostgresDockerRule POSTGRES = new PostgresDockerRule(VERSION);
    private static final DropwizardTestSupport<PublicAuthConfiguration> TEST_SUPPORT = new DropwizardTestSupport<>(PublicAuthApp.class, CONFIG_FILE_PATH,
            config("database.url", POSTGRES::getConnectionUrl),
            config("database.user", POSTGRES::getUsername),
            config("database.password", POSTGRES::getPassword));
    private DatabaseTestHelper databaseHelper;

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    startApplication();
                    doMigration();
                    databaseHelper = new DatabaseTestHelper(getJdbi());
                    restoreDropwizardsLogging();
                    base.evaluate();
                } finally {
                    stopApplication();
                }
            }
        };
    }

    private void startApplication() throws Exception {
        TEST_SUPPORT.before();
        TEST_SUPPORT.<PublicAuthApp>getApplication().run("db", "drop-all", "--confirm-delete-everything", CONFIG_FILE_PATH);
    }

    private void stopApplication() throws Exception {
        TEST_SUPPORT.after();
    }

    private void doMigration() throws SQLException, LiquibaseException {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getConnectionUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        )) {
            LOGGER.info("Running migrations.");
            Liquibase migrator = new Liquibase("it-migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator.update("");
        }
    }

    public Jdbi getJdbi() {
        return TEST_SUPPORT.<PublicAuthApp>getApplication().getJdbi();
    }

    public int getLocalPort() {
        return TEST_SUPPORT.getLocalPort();
    }

    public void stopPostgres() {
        PostgresTestHelper.stop();
    }

    private void restoreDropwizardsLogging() {
        TEST_SUPPORT.getConfiguration().getLoggingFactory().configure(
                TEST_SUPPORT.getEnvironment().metrics(), TEST_SUPPORT.getApplication().getName()
        );
    }

    public DatabaseTestHelper getDatabaseHelper() {
        return databaseHelper;
    }
}
