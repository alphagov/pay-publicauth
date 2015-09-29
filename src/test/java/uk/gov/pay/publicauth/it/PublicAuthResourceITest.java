package uk.gov.pay.publicauth.it;

import com.jayway.restassured.response.ValidatableResponse;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresRule;

import static com.jayway.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.core.Is.is;

public class PublicAuthResourceITest {

    private static final String BEARER_TOKEN = "TEST-BEARER-TOKEN";
    private static final String AUTH_PATH = "/v1/auth";
    private static final String ACCOUNT_ID = "ACCOUNT-ID";

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Test
    public void respondWith200_whenAuthWithVValidToken() throws Exception {
        app.getDatabaseHelper().insertAccount(BEARER_TOKEN, ACCOUNT_ID);
        authResponse().statusCode(200).body("account_id", is(ACCOUNT_ID));

    }

    private ValidatableResponse authResponse() {
        return given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + BEARER_TOKEN)
                .get(AUTH_PATH)
                .then();
    }

}
