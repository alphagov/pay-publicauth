package uk.gov.pay.publicauth.it;

import com.google.common.io.BaseEncoding;
import com.jayway.restassured.response.ValidatableResponse;
import org.apache.commons.codec.digest.HmacUtils;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresRule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;

public class PublicAuthResourceITest {

    private static final String SALT = "$2a$10$IhaXo6LIBhKIWOiGpbtPOu";
    private static final String BEARER_TOKEN = "TEST-BEARER-TOKEN";
    private static final String TOKEN_LINK = "123456789101112131415161718192021222";
    private static final String TOKEN_LINK_2 = "123456789101112131415161718192021223";
    private static final String HASHED_BEARER_TOKEN = BCrypt.hashpw(BEARER_TOKEN, SALT);
    private static final String HASHED_BEARER_TOKEN_2 = BCrypt.hashpw(BEARER_TOKEN + "-2", SALT);
    private static final String API_AUTH_PATH = "/v1/api/auth";
    private static final String FRONTEND_AUTH_PATH = "/v1/frontend/auth";
    private static final String ACCOUNT_ID = "ACCOUNT-ID";
    private static final String ACCOUNT_ID_2 = "ACCOUNT-ID-2";
    private static final String TOKEN_DESCRIPTION = "TOKEN DESCRIPTION";
    private static final String TOKEN_DESCRIPTION_2 = "Token description 2";

    private DateTime now = DateTime.now();

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Test
    public void respondWith200_whenAuthWithValidToken() throws Exception {

        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);

        tokenResponse(apiKey)
                .statusCode(200)
                .body("account_id", is(ACCOUNT_ID));
    }

    @Test
    public void respondWith200_whenCreateAToken_ifProvidedBothAccountIdAndDescription() throws Exception {

        String newToken = createTokenFor("{\"account_id\" : \"" + ACCOUNT_ID + "\", \"description\" : \"" + TOKEN_DESCRIPTION + "\"}")
                                .statusCode(200)
                                .body("token", is(notNullValue()))
                                .extract().path("token");

        int apiKeyHashSize = 32;
        String tokenApiKey = newToken.substring(0, newToken.length() - apiKeyHashSize);
        String hashedToken = BCrypt.hashpw(tokenApiKey, SALT);

        Optional<String> newTokenDescription = app.getDatabaseHelper().lookupColumnFor("description", "token_hash", hashedToken);

        assertThat(newTokenDescription.get(), equalTo(TOKEN_DESCRIPTION));

        Optional<String> newTokenAccountId = app.getDatabaseHelper().lookupColumnFor("account_id", "token_hash", hashedToken);
        assertThat(newTokenAccountId.get(), equalTo(ACCOUNT_ID));
    }

    @Test
    public void respondWith400_ifAccountAndDescriptionAreMissing() throws Exception {
        createTokenFor("{}")
                .statusCode(400)
                .body("message", is("Missing fields: [account_id, description]"));
    }

    @Test
    public void respondWith400_ifAccountIsMissing() throws Exception {
        createTokenFor("{\"description\" : \"" + TOKEN_DESCRIPTION + "\"}")
                .statusCode(400)
                .body("message", is("Missing fields: [account_id]"));
    }

    @Test
    public void respondWith400_ifDescriptionIsMissing() throws Exception {
        createTokenFor("{\"account_id\" : \"" + ACCOUNT_ID + "\"}")
                .statusCode(400)
                .body("message", is("Missing fields: [description]"));
    }

    @Test
    public void respondWith400_ifBodyIsMissing() throws Exception {
        createTokenFor("")
                .statusCode(400)
                .body("message", is("Missing fields: [account_id, description]"));
    }

    @Test
    public void respondWith200_andEmptyList_ifNoTokensHaveBeenIssuedForTheAccount() throws Exception {
        getTokensFor(ACCOUNT_ID)
                .statusCode(200)
                .body("tokens", hasSize(0));
    }

    @Test
    public void respondWith200_ifTokensHaveBeenIssuedForTheAccount() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2);

        List<Map<String,String>> retrievedTokens = getTokensFor(ACCOUNT_ID)
                .statusCode(200)
                .body("tokens", hasSize(2))
                .extract().path("tokens");

        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.size(), is(2));
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));

        Map<String, String> secondToken = retrievedTokens.get(1);
        assertThat(secondToken.size(), is(2));
        assertThat(secondToken.get("token_link"), is(TOKEN_LINK));
        assertThat(secondToken.get("description"), is(TOKEN_DESCRIPTION));
        assertThat(firstToken.containsKey("revoked"), is(false));
    }

    @Test
    public void respondWith200_butDoNotIncludeRevokedTokens() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, true);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2);

        List<Map<String,String>> retrievedTokens = getTokensFor(ACCOUNT_ID)
                .statusCode(200)
                .body("tokens", hasSize(2))
                .extract().path("tokens");

        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));

        Map<String, String> secondToken = retrievedTokens.get(1);
        assertThat(secondToken.get("token_link"), is(TOKEN_LINK));
        assertThat(secondToken.get("description"), is(TOKEN_DESCRIPTION));
        assertThat(secondToken.get("revoked"), is(now.toString("dd MMM YYYY")));
    }

    @Test
    public void respondWith400_ifNotProvidingDescription_whenUpdating() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK + "\"}")
            .statusCode(400)
            .body("message", is("Missing fields: [description]"));

        Optional<String> tokenLinkInDb = app.getDatabaseHelper().lookupColumnFor("token_link", "token_link", TOKEN_LINK);
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnFor("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK));
    }

    @Test
    public void respondWith400_ifNotProvidingTokenLink_whenUpdating() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        updateTokenDescription("{\"description\" : \"" + TOKEN_DESCRIPTION + "\"}")
            .statusCode(400)
            .body("message", is("Missing fields: [token_link]"));

        Optional<String> tokenLinkInDb = app.getDatabaseHelper().lookupColumnFor("token_link", "token_link", TOKEN_LINK);
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnFor("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK));
    }

    @Test
    public void respondWith400_ifNotProvidingTokenLinkNorDescription_whenUpdating() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        updateTokenDescription("{}")
            .statusCode(400)
            .body("message", is("Missing fields: [token_link, description]"));

        Optional<String> tokenLinkInDb = app.getDatabaseHelper().lookupColumnFor("token_link", "token_link", TOKEN_LINK);
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnFor("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK));
    }

    @Test
    public void respondWith400_ifNotProvidingBody_whenUpdating() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        updateTokenDescription("")
            .statusCode(400)
            .body("message", is("Missing fields: [token_link, description]"));

        Optional<String> tokenLinkInDb = app.getDatabaseHelper().lookupColumnFor("token_link", "token_link", TOKEN_LINK);
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnFor("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK));
    }

    @Test
    public void respondWith200_ifUpdatingDescriptionOfExistingToken() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK + "\", \"description\" : \"" + TOKEN_DESCRIPTION_2 + "\"}")
            .statusCode(200)
            .body("token_link", is(TOKEN_LINK))
            .body("description", is(TOKEN_DESCRIPTION_2));

        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnFor("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION_2));
    }

    @Test
    public void respondWith404_ifUpdatingDescriptionOfNonExistingToken() throws Exception {
        updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK + "\", \"description\" : \"" + TOKEN_DESCRIPTION_2 + "\"}")
            .statusCode(404)
            .body("message", is("Could not update token description"));

        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnFor("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith404_butDoNotUpdateRevokedTokens() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, true);

        updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK + "\", \"description\" : \"" + TOKEN_DESCRIPTION_2 + "\"}")
            .statusCode(404)
            .body("message", is("Could not update token description"));

        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnFor("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
    }

    @Test
    public void respondWith400_ifNotProvidingBody_whenRevokingAToken() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        revokeSingleToken(ACCOUNT_ID, "")
                .statusCode(400)
                .body("message", is("Missing fields: [token_link]"));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnFor("revoked", "token_link", TOKEN_LINK);
        assertThat(revokedInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith400_ifProvidingEmptyBody_whenRevokingAToken() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        revokeSingleToken(ACCOUNT_ID, "{}")
                .statusCode(400)
                .body("message", is("Missing fields: [token_link]"));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnFor("revoked", "token_link", TOKEN_LINK);
        assertThat(revokedInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith200_whenSingleTokenIsRevoked() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK + "\"}")
            .statusCode(200)
            .body("revoked", is(now.toString("dd MMM YYYY")));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnFor("revoked", "token_link", TOKEN_LINK);
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    public void respondWith404_whenRevokingTokenForAnotherAccount() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID_2, TOKEN_DESCRIPTION);

        revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK_2 + "\"}")
            .statusCode(404)
            .body("message", is("Could not revoke token"));

        Optional<String> token1RevokedInDb = app.getDatabaseHelper().lookupColumnFor("revoked", "token_link", TOKEN_LINK);
        assertThat(token1RevokedInDb.isPresent(), is(false));
        Optional<String> token2RevokedInDb = app.getDatabaseHelper().lookupColumnFor("revoked", "token_link", TOKEN_LINK_2);
        assertThat(token2RevokedInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith404_whenRevokingTokenAlreadyRevoked() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, true);

        revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK + "\"}")
            .statusCode(404)
            .body("message", is("Could not revoke token"));

        Optional<String> token1RevokedInDb = app.getDatabaseHelper().lookupColumnFor("revoked", "token_link", TOKEN_LINK);
        assertThat(token1RevokedInDb.isPresent(), is(true));
    }

   @Test
    public void respondWith404_whenTokenDoesNotExist() throws Exception {
        revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK + "\"}")
            .statusCode(404)
            .body("message", is("Could not revoke token"));

        Optional<String> tokenLinkdInDb = app.getDatabaseHelper().lookupColumnFor("token_link", "token_link", TOKEN_LINK);
        assertThat(tokenLinkdInDb.isPresent(), is(false));
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

        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);

        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Basic " + apiKey)
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

        Optional<String> storedTokenHash = app.getDatabaseHelper().lookupColumnFor("token_hash", "account_id", ACCOUNT_ID);
        assertThat(storedTokenHash.get(), is(not(newToken)));
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
                .get(FRONTEND_AUTH_PATH + "/" + accountId)
                .then();
    }

    private ValidatableResponse updateTokenDescription(String body) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .body(body)
                .put(FRONTEND_AUTH_PATH)
                .then();
    }

    private ValidatableResponse revokeSingleToken(String accountId, String body) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .contentType(JSON)
                .body(body)
                .delete(FRONTEND_AUTH_PATH + "/" + accountId)
                .then();
    }

    private ValidatableResponse tokenResponse(String token) {
        return given()
                .port(app.getLocalPort())
                .header(AUTHORIZATION, "Bearer " + token)
                .get(API_AUTH_PATH)
                .then();
    }

    private String encodedHmacValueOf(String input) {
        return BaseEncoding.base32Hex().lowerCase().omitPadding().encode(HmacUtils.hmacSha1("qwer9yuhgf", input));
    }
}
