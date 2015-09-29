package uk.gov.pay.publicauth.it;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.ValidatableResponse;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresRule;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

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

    @Test
    public void respondWith200_whenCreateAAccountWithAToken() throws Exception {
        authCreateResponse("{\"account_id\" : \"" + ACCOUNT_ID + "\"}")
                .statusCode(200).body("token", is(notNullValue()));

    }

    @Test
    public void respondWith400_ifAccountIdIsMissing() throws Exception {
        authCreateResponse("{}")
                .statusCode(400).body("message", is("Missing fields: [account_id]"));
    }

    @Test
    public void respondWith400_ifBodyIsMissing() throws Exception {
        authCreateResponse("")
                .statusCode(400).body("message", is("Missing fields: [account_id]"));
    }

    private ValidatableResponse authCreateResponse(String body) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .body(body)
                .post(AUTH_PATH)
                .then();
    }

    private ValidatableResponse authResponse() {
        return given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + BEARER_TOKEN)
                .get(AUTH_PATH)
                .then();
    }

}
