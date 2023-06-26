package uk.gov.pay.publicauth.it;

import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import uk.gov.pay.publicauth.app.PublicAuthApp;
import uk.gov.pay.publicauth.app.config.PublicAuthConfiguration;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresExtension;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresExtension.TEST_CONFIG_FILE_PATH;
import static uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresExtension.VERSION;

/*
Because this test messes with the container lifecycle to validate application health,
we need to create a separate Postgres container that won't affect other tests when run in sequence
 */
class HealthCheckResourceIT {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(format("postgres:%s", VERSION))
            .withDatabaseName("publicauth_test")
            .withUsername("test")
            .withPassword("test");
    private static DropwizardTestSupport<PublicAuthConfiguration> TEST_SUPPORT;

    @BeforeAll
    public static void setup() throws Exception {
        POSTGRES.start();
        TEST_SUPPORT = new DropwizardTestSupport<>(PublicAuthApp.class, TEST_CONFIG_FILE_PATH,
                config("database.url", POSTGRES::getJdbcUrl),
                config("database.user", POSTGRES::getUsername),
                config("database.password", POSTGRES::getPassword));
        TEST_SUPPORT.before();
    }

    @Test
    void checkHealthcheck_allIsHealthy() {
        given().port(TEST_SUPPORT.getLocalPort())
                .get("healthcheck")
                .then()
                .statusCode(200)
                .body("postgresql.healthy", is(true))
                .body("deadlocks.healthy", is(true));
    }

    @Test
    void checkHealthCheck_isUnHealthy() {
        POSTGRES.stop();
        given().port(TEST_SUPPORT.getLocalPort())
                .get("healthcheck")
                .then()
                .statusCode(503)
                .body("postgresql.healthy", is(false))
                .body("deadlocks.healthy", is(true));
    }
}
