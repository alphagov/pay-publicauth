package uk.gov.pay.publicauth.it;

import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresRule;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.publicauth.resources.HealthCheckResource.HEALTHCHECK;

public class HealthCheckResourceITest {

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Test
    public void checkHealthcheck() throws Exception {
        given().port(app.getLocalPort())
                .get(HEALTHCHECK)
                .then()
                .statusCode(200)
                .body("postgresql.healthy", is(true))
                .body("deadlocks.healthy", is(true));
    }
}
