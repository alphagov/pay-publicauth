package uk.gov.pay.publicauth.it;

import com.jayway.restassured.response.ValidatableResponse;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.publicauth.service.TokenHasher;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresRule;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class PublicAuthResourceITest {

    private static final String BEARER_TOKEN = "TEST-BEARER-TOKEN";
    private static final String HASHED_BEARER_TOKEN = new TokenHasher().hash(BEARER_TOKEN);
    private static final String AUTH_PATH = "/v1/auth";
    private static final String ACCOUNT_ID = "ACCOUNT-ID";

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Test
    public void respondWith200_whenAuthWithValidToken() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, ACCOUNT_ID);
        tokenResponse().statusCode(200).body("account_id", is(ACCOUNT_ID));

    }

    @Test
    public void respondWith200_whenCreateAccountWithAToken() throws Exception {
        createTokenFor("{\"account_id\" : \"" + ACCOUNT_ID + "\"}")
                .statusCode(200).body("token", is(notNullValue()));
    }

    @Test
    public void respondWith400_ifAccountIdIsMissing() throws Exception {
        createTokenFor("{}")
                .statusCode(400).body("message", is("Missing fields: [account_id]"));
    }

    @Test
    public void respondWith400_ifBodyIsMissing() throws Exception {
        createTokenFor("")
                .statusCode(400).body("message", is("Missing fields: [account_id]"));
    }

    @Test
    public void respondWith200_whenTokenIsRevoked() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, ACCOUNT_ID);

        authRevokeResponse().statusCode(200);

        tokenResponse().statusCode(401);
    }

    @Test
    public void respondWith200_whenMultipleTokensAreRevoked() throws Exception {
        String second_bearer_token = new TokenHasher().hash("SECOND_BEARER_TOKEN");
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, ACCOUNT_ID);
        app.getDatabaseHelper().insertAccount(second_bearer_token, ACCOUNT_ID);

        authRevokeResponse().statusCode(200);

        tokenResponse().statusCode(401);
        tokenResponse(second_bearer_token).statusCode(401);
    }
    
    @Test
    public void respondWith404_whenAccountIsMissing() throws Exception {
        authRevokeResponse().statusCode(404);
    }

    @Test
    public void respondWith401_whenAuthHeaderIsMissing() throws Exception {
        given().port(app.getLocalPort())
                .get(AUTH_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    public void respondWith401_whenAuthHeaderIsBasicEvenWithValidToken() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, ACCOUNT_ID);

        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Basic " + BEARER_TOKEN)
                .get(AUTH_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    public void shouldNotStoreTheTokenInThePlain() throws Exception {
        String token = createTokenFor("{\"account_id\" : \"" + ACCOUNT_ID + "\"}")
                .statusCode(200)
                .extract()
                .body()
                .path("token");
        String storedToken = app.getDatabaseHelper().lookupTokenFor(ACCOUNT_ID);
        assertThat(storedToken, is(not(token)));
    }


    private ValidatableResponse authRevokeResponse() {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .post(AUTH_PATH + "/" + ACCOUNT_ID + "/revoke")
                .then();
    }

    private ValidatableResponse createTokenFor(String body) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .body(body)
                .post(AUTH_PATH)
                .then();
    }

    private ValidatableResponse tokenResponse() {
        return tokenResponse(BEARER_TOKEN);
    }

    private ValidatableResponse tokenResponse(String token) {
        return given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + token)
                .get(AUTH_PATH)
                .then();
    }

}
