package uk.gov.pay.publicauth.it;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresRule;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.publicauth.resources.HealthCheckResource.HEALTHCHECK;

@RunWith(MockitoJUnitRunner.class)
public class HealthCheckResourceITest {

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Test
    public void checkHealthcheck_allIsHealthy() throws Exception {
        given().port(app.getLocalPort())
                .get(HEALTHCHECK)
                .then()
                .statusCode(200)
                .body("postgresql.healthy", is(true))
                .body("deadlocks.healthy", is(true));
    }

    @Test
    public void checkHealthCheck_isUnHealthy() throws Exception {
        app.stopPostgres();
        given().port(app.getLocalPort())
                .get(HEALTHCHECK)
                .then()
                .statusCode(503)
                .body("postgresql.healthy", is(false))
                .body("deadlocks.healthy", is(true));
    }
}
