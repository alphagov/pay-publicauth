package uk.gov.pay.publicauth.it;

import com.jayway.restassured.response.ValidatableResponse;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.publicauth.service.TokenHasher;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresRule;

import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class PublicAuthResourceITest {

    private static final String BEARER_TOKEN = "TEST-BEARER-TOKEN";
    private static final String HASHED_BEARER_TOKEN = new TokenHasher().hash(BEARER_TOKEN);
    private static final String API_AUTH_PATH = "/v1/api/auth";
    private static final String FRONTEND_AUTH_PATH = "/v1/frontend/auth";
    private static final String ACCOUNT_ID = "ACCOUNT-ID";
    private static final String TOKEN_DESCRIPTION = "TOKEN DESCRIPTION";

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Test
    public void respondWith200_whenAuthWithValidToken() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, ACCOUNT_ID, TOKEN_DESCRIPTION);
        tokenResponse().statusCode(200).body("account_id", is(ACCOUNT_ID));

    }

    @Test
    public void respondWith200_whenCreateAToken_ifProvidedBothAccountIdAndDescription() throws Exception {
        String newToken = createTokenFor("{\"account_id\" : \"" + ACCOUNT_ID + "\", \"description\" : \"" + TOKEN_DESCRIPTION + "\"}")
                .statusCode(200)
                .body("token", is(notNullValue()))
                .extract().path("token");

        String newTokenDescription = app.getDatabaseHelper().getColumnForTokenHash("description", new TokenHasher().hash(newToken));
        assertThat(newTokenDescription, equalTo(TOKEN_DESCRIPTION));

        String newTokenAccountId = app.getDatabaseHelper().getColumnForTokenHash("account_id", new TokenHasher().hash(newToken));
        assertThat(newTokenAccountId, equalTo(ACCOUNT_ID));

    }

    @Test
    public void respondWith400_ifAccountAndDescriptionAreMissing() throws Exception {
        createTokenFor("{}")
                .statusCode(400).body("message", is("Missing fields: [account_id, description]"));
    }

    @Test
    public void respondWith400_ifAccountIsMissing() throws Exception {
        createTokenFor("{\"description\" : \"" + TOKEN_DESCRIPTION + "\"}")
                .statusCode(400).body("message", is("Missing fields: [account_id]"));
    }

    @Test
    public void respondWith400_ifDescriptionIsMissing() throws Exception {
        createTokenFor("{\"account_id\" : \"" + ACCOUNT_ID + "\"}")
                .statusCode(400).body("message", is("Missing fields: [description]"));
    }

    @Test
    public void respondWith400_ifBodyIsMissing() throws Exception {
        createTokenFor("")
                .statusCode(400).body("message", is("Missing fields: [account_id, description]"));
    }

    @Test
    public void respondWith200_andEmptyList_ifNoTokensHaveBeenIssuedForTheAccount() throws Exception {
        getTokensFor(ACCOUNT_ID)
                .statusCode(200).body("tokens", hasSize(0));
    }

    @Test
    public void respondWith200_ifTokensHaveBeenIssuedForTheAccount() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, ACCOUNT_ID, TOKEN_DESCRIPTION);
        String secondHashedToken = new TokenHasher().hash(BEARER_TOKEN + "-2");
        app.getDatabaseHelper().insertAccount(secondHashedToken, ACCOUNT_ID, TOKEN_DESCRIPTION + " 2");

        Map<String, String> expectedMapForFirstToken = new HashMap<>();
        expectedMapForFirstToken.put("token_hash", HASHED_BEARER_TOKEN);
        expectedMapForFirstToken.put("description", TOKEN_DESCRIPTION);

        Map<String, String> expectedMapForSecondToken = new HashMap<>();
        expectedMapForSecondToken.put("token_hash", secondHashedToken);
        expectedMapForSecondToken.put("description", TOKEN_DESCRIPTION + " 2");

        getTokensFor(ACCOUNT_ID)
                .statusCode(200)
                .body("tokens", containsInAnyOrder(expectedMapForFirstToken, expectedMapForSecondToken));
    }

    @Test
    public void respondWith200_butDoNotIncludeRevokedTokens() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, ACCOUNT_ID, TOKEN_DESCRIPTION, true);
        String secondHashedToken = new TokenHasher().hash(BEARER_TOKEN + "-2");
        app.getDatabaseHelper().insertAccount(secondHashedToken, ACCOUNT_ID, TOKEN_DESCRIPTION + " 2");

        Map<String, String> expectedMapForToken = new HashMap<>();
        expectedMapForToken.put("token_hash", secondHashedToken);
        expectedMapForToken.put("description", TOKEN_DESCRIPTION + " 2");

        getTokensFor(ACCOUNT_ID)
                .statusCode(200)
                .body("tokens", containsInAnyOrder(expectedMapForToken));
    }

    @Test
    public void respondWith200_whenTokenIsRevoked() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, ACCOUNT_ID, TOKEN_DESCRIPTION);

        authRevokeResponse().statusCode(200);

        tokenResponse().statusCode(401);
    }

    @Test
    public void respondWith200_whenMultipleTokensAreRevoked() throws Exception {
        String second_bearer_token = new TokenHasher().hash("SECOND_BEARER_TOKEN");
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, ACCOUNT_ID, TOKEN_DESCRIPTION);
        app.getDatabaseHelper().insertAccount(second_bearer_token, ACCOUNT_ID, TOKEN_DESCRIPTION);

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
                .get(API_AUTH_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    public void respondWith401_whenAuthHeaderIsBasicEvenWithValidToken() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, ACCOUNT_ID, TOKEN_DESCRIPTION);

        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Basic " + BEARER_TOKEN)
                .get(API_AUTH_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    public void shouldNotStoreTheTokenInThePlain() throws Exception {
        String newToken = createTokenFor("{\"account_id\" : \"" + ACCOUNT_ID + "\", \"description\" : \"" + TOKEN_DESCRIPTION + "\"}")
                .statusCode(200)
                .extract()
                .body()
                .path("token");
        String storedToken = app.getDatabaseHelper().lookupTokenFor(ACCOUNT_ID);
        assertThat(storedToken, is(not(newToken)));
    }


    private ValidatableResponse authRevokeResponse() {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .post(FRONTEND_AUTH_PATH + "/" + ACCOUNT_ID + "/revoke")
                .then();
    }

    private ValidatableResponse createTokenFor(String body) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .body(body)
                .post(FRONTEND_AUTH_PATH)
                .then();
    }

    private ValidatableResponse getTokensFor(String accountId) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .get(FRONTEND_AUTH_PATH + "/" + accountId)
                .then();
    }

    private ValidatableResponse tokenResponse() {
        return tokenResponse(BEARER_TOKEN);
    }

    private ValidatableResponse tokenResponse(String token) {
        return given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + token)
                .get(API_AUTH_PATH)
                .then();
    }

}
