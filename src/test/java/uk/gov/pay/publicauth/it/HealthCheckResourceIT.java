package uk.gov.pay.publicauth.it;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.publicauth.app.PublicAuthApp;
import uk.gov.pay.publicauth.app.config.PublicAuthConfiguration;
import uk.gov.pay.publicauth.utils.PostgresTestContainersBase;


import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

public class HealthCheckResourceIT extends PostgresTestContainersBase {

    @Rule
    public DropwizardAppRule<PublicAuthConfiguration> app = new DropwizardAppRule<>(
            PublicAuthApp.class, resourceFilePath("config/test-it-config.yaml"), 
                config("database.url", POSTGRES_CONTAINER.getJdbcUrl()),
                config("database.user", POSTGRES_CONTAINER.getUsername()),
                config("database.password", POSTGRES_CONTAINER.getPassword()));
    
    @Test
    public void checkHealthcheck_allIsHealthy() {
        given().port(app.getLocalPort())
                .get("healthcheck")
                .then()
                .statusCode(200)
                .body("postgresql.healthy", is(true))
                .body("deadlocks.healthy", is(true));
    }

    @Test
    public void checkHealthCheck_isUnHealthy() {
        POSTGRES_CONTAINER.stop();
        given().port(app.getLocalPort())
                .get("healthcheck")
                .then()
                .statusCode(503)
                .body("postgresql.healthy", is(false))
                .body("deadlocks.healthy", is(true));
    }
}
