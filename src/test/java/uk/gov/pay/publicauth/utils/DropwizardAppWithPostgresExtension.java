package uk.gov.pay.publicauth.utils;

import io.dropwizard.testing.DropwizardTestSupport;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.pay.publicauth.app.PublicAuthApp;
import uk.gov.pay.publicauth.app.config.PublicAuthConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.lang.String.format;

public class DropwizardAppWithPostgresExtension implements BeforeAllCallback, AfterEachCallback, AfterAllCallback, ParameterResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DropwizardAppWithPostgresExtension.class);
    public static final String TEST_CONFIG_FILE_PATH = resourceFilePath("config/test-it-config.yaml");
    public static final String VERSION = "15.2";
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(format("postgres:%s", VERSION))
            .withDatabaseName("publicauth_test")
            .withUsername("test")
            .withPassword("test");
    private static final DropwizardTestSupport<PublicAuthConfiguration> TEST_SUPPORT = new DropwizardTestSupport<>(PublicAuthApp.class, TEST_CONFIG_FILE_PATH,
            config("database.url", POSTGRES::getJdbcUrl),
            config("database.user", POSTGRES::getUsername),
            config("database.password", POSTGRES::getPassword));
    private DatabaseTestHelper databaseHelper;
    private PublicAuthApp application;

    static {
        POSTGRES.start();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        LOGGER.info("Starting application");
        TEST_SUPPORT.before();
        application = TEST_SUPPORT.getApplication();
        databaseHelper = new DatabaseTestHelper(getJdbi());
        doMigration();
        restoreDropwizardLogging();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        databaseHelper.truncateDatabase();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        TEST_SUPPORT.after();
        application = null;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return parameterType.equals(PublicAuthApp.class) ||
                parameterType.equals(DatabaseTestHelper.class) ||
                parameterType.equals(Integer.class) ||
                parameterType.equals(PostgreSQLContainer.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        if (parameterType.equals(PublicAuthApp.class)) {
            return application;
        } else if (parameterType.equals(DatabaseTestHelper.class)) {
            return databaseHelper;
        } else if (parameterType.equals(Integer.class)) {
            return TEST_SUPPORT.getLocalPort();
        } else if (parameterType.equals(PostgreSQLContainer.class)) {
            return POSTGRES;
        }
        throw new IllegalArgumentException("Unsupported parameter type: " + parameterType);
    }

    private void doMigration() throws SQLException, LiquibaseException {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        )) {
            LOGGER.info("Running migrations");
            Liquibase migrator = new Liquibase("it-migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator.update("");
        }
    }

    private Jdbi getJdbi() {
        return TEST_SUPPORT.<PublicAuthApp>getApplication().getJdbi();
    }

    private void restoreDropwizardLogging() {
        TEST_SUPPORT.getConfiguration().getLoggingFactory().configure(
                TEST_SUPPORT.getEnvironment().metrics(), TEST_SUPPORT.getApplication().getName()
        );
    }
}
