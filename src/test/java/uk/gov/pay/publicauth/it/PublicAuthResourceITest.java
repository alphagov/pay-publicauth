package uk.gov.pay.publicauth.it;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.jayway.restassured.response.ValidatableResponse;
import org.apache.commons.codec.digest.HmacUtils;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
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
import static org.joda.time.DateTimeZone.*;
import static uk.gov.pay.publicauth.resources.PublicAuthResource.ACCOUNT_ID_FIELD;
import static uk.gov.pay.publicauth.resources.PublicAuthResource.CREATED_BY_FIELD;
import static uk.gov.pay.publicauth.resources.PublicAuthResource.DESCRIPTION_FIELD;

public class PublicAuthResourceITest {

    private static final String SALT = "$2a$10$IhaXo6LIBhKIWOiGpbtPOu";
    private static final String BEARER_TOKEN = "testbearertoken";
    private static final String TOKEN_LINK = "123456789101112131415161718192021222";
    private static final String TOKEN_LINK_2 = "123456789101112131415161718192021223";
    private static final String HASHED_BEARER_TOKEN = BCrypt.hashpw(BEARER_TOKEN, SALT);
    private static final String HASHED_BEARER_TOKEN_2 = BCrypt.hashpw(BEARER_TOKEN + "2", SALT);
    private static final String API_AUTH_PATH = "/v1/api/auth";
    private static final String FRONTEND_AUTH_PATH = "/v1/frontend/auth";
    private static final String ACCOUNT_ID = "ACCOUNT-ID";
    private static final String ACCOUNT_ID_2 = "ACCOUNT-ID-2";
    private static final String TOKEN_DESCRIPTION = "TOKEN DESCRIPTION";
    private static final String TOKEN_DESCRIPTION_2 = "Token description 2";
    public static final String USER_EMAIL = "user@email.com";
    public static final String TOKEN_HASH_COLUMN = "token_hash";
    public static final String CREATED_USER_NAME = "user-name";
    public static final String CREATED_USER_NAME2 = "user-name-2";

    private DateTime now = DateTime.now();

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();
    private String validTokenPayload = new Gson().toJson(
            ImmutableMap.of("account_id", ACCOUNT_ID,
                    "description", TOKEN_DESCRIPTION,
                    "created_by", USER_EMAIL));

    @Test
    public void respondWith200_whenAuthWithValidToken() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
        String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);
        tokenResponse(apiKey)
                .statusCode(200)
                .body("account_id", is(ACCOUNT_ID));
        DateTime lastUsed = app.getDatabaseHelper().getDateTimeColumn("last_used", ACCOUNT_ID);
        DateTime currentTimeInDb = app.getDatabaseHelper().getCurrentTime();
        assertThat(lastUsed, isCloseTo(currentTimeInDb));
    }

    @Test
    public void respondWith401_whenAuthWithRevokedToken() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, DateTime.now(), CREATED_USER_NAME);
        String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);
        DateTime lastUsedPreAuth = app.getDatabaseHelper().getDateTimeColumn("last_used", ACCOUNT_ID);
        tokenResponse(apiKey)
                .statusCode(401);
        DateTime lastUsedPostAuth = app.getDatabaseHelper().getDateTimeColumn("last_used", ACCOUNT_ID);

        assertThat(lastUsedPreAuth, is(lastUsedPostAuth));
    }

    @Test
    public void respondWith401_whenAuthWithNonExistentToken() throws Exception {
        String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);
        tokenResponse(apiKey)
                .statusCode(401);
    }

    @Test
    public void respondWith200_whenCreateAToken_ifProvidedBothAccountIdAndDescription() throws Exception {
        String newToken = createTokenFor(validTokenPayload)
                .statusCode(200)
                .body("token", is(notNullValue()))
                .extract().path("token");

        int apiKeyHashSize = 32;
        String tokenApiKey = newToken.substring(0, newToken.length() - apiKeyHashSize);
        String hashedToken = BCrypt.hashpw(tokenApiKey, SALT);

        Optional<String> newTokenDescription = app.getDatabaseHelper().lookupColumnForTokenTable(DESCRIPTION_FIELD, TOKEN_HASH_COLUMN, hashedToken);

        assertThat(newTokenDescription.get(), equalTo(TOKEN_DESCRIPTION));

        Optional<String> newTokenAccountId = app.getDatabaseHelper().lookupColumnForTokenTable(ACCOUNT_ID_FIELD, TOKEN_HASH_COLUMN, hashedToken);
        assertThat(newTokenAccountId.get(), equalTo(ACCOUNT_ID));

        Optional<String> newCreatedByEmail = app.getDatabaseHelper().lookupColumnForTokenTable(CREATED_BY_FIELD, TOKEN_HASH_COLUMN, hashedToken);
        assertThat(newCreatedByEmail.get(), equalTo(USER_EMAIL));
    }

    @Test
    public void respondWith400_ifAccountAndDescriptionAreMissing() throws Exception {
        createTokenFor("{}")
                .statusCode(400)
                .body("message", is("Missing fields: [account_id, description, created_by]"));
    }

    @Test
    public void respondWith400_ifAccountIsMissing() throws Exception {
        createTokenFor("{\"description\" : \"" + ACCOUNT_ID + "\", \"created_by\": \"some-user\"}")
                .statusCode(400)
                .body("message", is("Missing fields: [account_id]"));
    }

    @Test
    public void respondWith400_ifDescriptionIsMissing() throws Exception {
        createTokenFor("{\"account_id\" : \"" + ACCOUNT_ID + "\", \"created_by\": \"some-user\"}")
                .statusCode(400)
                .body("message", is("Missing fields: [description]"));
    }

    @Test
    public void respondWith400_ifBodyIsMissing() throws Exception {
        createTokenFor("")
                .statusCode(400)
                .body("message", is("Missing fields: [account_id, description, created_by]"));
    }

    @Test
    public void respondWith200_andEmptyList_ifNoTokensHaveBeenIssuedForTheAccount() throws Exception {
        getTokensFor(ACCOUNT_ID)
                .statusCode(200)
                .body("tokens", hasSize(0));
    }

    @Test
    public void respondWith200_ifTokensHaveBeenIssuedForTheAccount() throws Exception {
        DateTime inserted = app.getDatabaseHelper().getCurrentTime().toDateTime(UTC);
        DateTime lastUsed = inserted.plusHours(1);
        DateTime revoked = new DateTime(UTC)
                .plusDays(1)
                .withHourOfDay(00)
                .withMinuteOfHour(20)
                .withSecondOfMinute(0);

        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, null, CREATED_USER_NAME, lastUsed);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);

        List<Map<String, String>> retrievedTokens = getTokensFor(ACCOUNT_ID)
                .statusCode(200)
                .body("tokens", hasSize(2))
                .extract().path("tokens");


        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.size(), is(5));
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));
        assertThat(firstToken.get("created_by"), is(CREATED_USER_NAME2));
        assertThat(firstToken.get("issued_date"), is(inserted.toString("dd MMM YYYY - HH:mm")));
        assertThat(firstToken.get("last_used"), is(lastUsed.toString("dd MMM YYYY - HH:mm")));

        Map<String, String> secondToken = retrievedTokens.get(1);
        assertThat(secondToken.size(), is(5));
        assertThat(secondToken.get("token_link"), is(TOKEN_LINK));
        assertThat(secondToken.get("description"), is(TOKEN_DESCRIPTION));
        assertThat(secondToken.containsKey("revoked"), is(false));
        assertThat(secondToken.get("created_by"), is(CREATED_USER_NAME));
        assertThat(secondToken.get("issued_date"), is(inserted.toString("dd MMM YYYY - HH:mm")));
        assertThat(secondToken.get("last_used"), is(lastUsed.toString("dd MMM YYYY - HH:mm")));
    }

    @Test
    public void respondWith200_andRetrieveRevokedTokens() throws Exception {
        DateTime inserted = app.getDatabaseHelper().getCurrentTime().toDateTime(DateTimeZone.UTC);
        DateTime lastUsed = inserted.plusHours(1);
        DateTime revoked = inserted.plusHours(2);

        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, CREATED_USER_NAME, lastUsed);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);

        List<Map<String, String>> retrievedTokens = getTokensFor(ACCOUNT_ID, "revoked")
                .statusCode(200)
                .body("tokens", hasSize(1))
                .extract().path("tokens");


        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION));
        assertThat(firstToken.get("revoked"), is(revoked.toString("dd MMM YYYY - HH:mm")));
        assertThat(firstToken.get("created_by"), is(CREATED_USER_NAME));
        assertThat(firstToken.get("last_used"), is(lastUsed.toString("dd MMM YYYY - HH:mm")));
        assertThat(firstToken.get("issued_date"), is(inserted.toString("dd MMM YYYY - HH:mm")));
    }

    @Test
    public void respondWith200_andRetrieveActiveTokens() throws Exception {
        DateTime inserted = app.getDatabaseHelper().getCurrentTime().toDateTime(DateTimeZone.UTC);
        DateTime lastUsed = inserted.plusHours(1);
        DateTime revoked = inserted.plusHours(2);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, CREATED_USER_NAME, lastUsed);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);

        List<Map<String, String>> retrievedTokens = getTokensFor(ACCOUNT_ID, "active")
                .statusCode(200)
                .body("tokens", hasSize(1))
                .extract().path("tokens");

        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));
        assertThat(firstToken.get("created_by"), is(CREATED_USER_NAME2));
        assertThat(firstToken.get("last_used"), is(lastUsed.toString("dd MMM YYYY - HH:mm")));
        assertThat(firstToken.get("issued_date"), is(inserted.toString("dd MMM YYYY - HH:mm")));
    }

    @Test
    public void respondWith200_andRetrieveActiveTokensIfNoQueryParamIsSpecified() throws Exception {
        DateTime inserted = app.getDatabaseHelper().getCurrentTime().toDateTime(UTC);
        DateTime lastUsed = inserted.plusHours(1);
        DateTime revoked = inserted.plusHours(2);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, CREATED_USER_NAME, lastUsed);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);

        List<Map<String, String>> retrievedTokens = getTokensForWithNoQueryParam(ACCOUNT_ID)
                .statusCode(200)
                .body("tokens", hasSize(1))
                .extract().path("tokens");

        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));
        assertThat(firstToken.get("created_by"), is(CREATED_USER_NAME2));
        assertThat(firstToken.get("last_used"), is(lastUsed.toString("dd MMM YYYY - HH:mm")));
        assertThat(firstToken.get("issued_date"), is(inserted.toString("dd MMM YYYY - HH:mm")));
    }

    @Test
    public void respondWith200_andRetrieveActiveTokensIfUnknownQueryParamIsSpecified() throws Exception {
        DateTime inserted = app.getDatabaseHelper().getCurrentTime().toDateTime(UTC);
        DateTime lastUsed = inserted.plusHours(1);
        DateTime revoked = inserted.plusHours(2);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, CREATED_USER_NAME, lastUsed);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);
        List<Map<String, String>> retrievedTokens = getTokensFor(ACCOUNT_ID, "something")
                .statusCode(200)
                .body("tokens", hasSize(1))
                .extract().path("tokens");

        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));
        assertThat(firstToken.get("created_by"), is(CREATED_USER_NAME2));
        assertThat(firstToken.get("last_used"), is(lastUsed.toString("dd MMM YYYY - HH:mm")));
        assertThat(firstToken.get("issued_date"), is(inserted.toString("dd MMM YYYY - HH:mm")));
    }

    @Test
    public void respondWith400_ifNotProvidingDescription_whenUpdating() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK + "\"}")
                .statusCode(400)
                .body("message", is("Missing fields: [description]"));

        Optional<String> tokenLinkInDb = app.getDatabaseHelper().lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK);
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK));
    }

    @Test
    public void respondWith400_ifNotProvidingTokenLink_whenUpdating() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        updateTokenDescription("{\"description\" : \"" + TOKEN_DESCRIPTION + "\"}")
                .statusCode(400)
                .body("message", is("Missing fields: [token_link]"));

        Optional<String> tokenLinkInDb = app.getDatabaseHelper().lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK);
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK));
    }

    @Test
    public void respondWith400_ifNotProvidingTokenLinkNorDescription_whenUpdating() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        updateTokenDescription("{}")
                .statusCode(400)
                .body("message", is("Missing fields: [token_link, description]"));

        Optional<String> tokenLinkInDb = app.getDatabaseHelper().lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK);
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK));
    }

    @Test
    public void respondWith400_ifNotProvidingBody_whenUpdating() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        updateTokenDescription("")
                .statusCode(400)
                .body("message", is("Missing fields: [token_link, description]"));

        Optional<String> tokenLinkInDb = app.getDatabaseHelper().lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK);
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK));
    }

    @Test
    public void respondWith200_ifUpdatingDescriptionOfExistingToken() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
        DateTime nowFromDB = app.getDatabaseHelper().getCurrentTime().toDateTime(UTC);

        updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK + "\", \"description\" : \"" + TOKEN_DESCRIPTION_2 + "\"}")
                .statusCode(200)
                .body("token_link", is(TOKEN_LINK))
                .body("description", is(TOKEN_DESCRIPTION_2))
                .body("issued_date", is(nowFromDB.toString("dd MMM YYYY - HH:mm")))
                .body("last_used", is(nowFromDB.toString("dd MMM YYYY - HH:mm")))
                .body("created_by", is(CREATED_USER_NAME));

        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION_2));
    }

    @Test
    public void respondWith404_ifUpdatingDescriptionOfNonExistingToken() throws Exception {
        updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK + "\", \"description\" : \"" + TOKEN_DESCRIPTION_2 + "\"}")
                .statusCode(404)
                .body("message", is("Could not update token description"));

        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith404_butDoNotUpdateRevokedTokens() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, DateTime.now(), CREATED_USER_NAME);

        updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK + "\", \"description\" : \"" + TOKEN_DESCRIPTION_2 + "\"}")
                .statusCode(404)
                .body("message", is("Could not update token description"));

        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
    }

    @Test
    public void respondWith400_ifNotProvidingBody_whenRevokingAToken() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        revokeSingleToken(ACCOUNT_ID, "")
                .statusCode(400)
                .body("message", is("Missing fields: [token_link]"));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK);
        assertThat(revokedInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith400_ifProvidingEmptyBody_whenRevokingAToken() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        revokeSingleToken(ACCOUNT_ID, "{}")
                .statusCode(400)
                .body("message", is("Missing fields: [token_link]"));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK);
        assertThat(revokedInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith200_whenSingleTokenIsRevoked() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK + "\"}")
                .statusCode(200)
                .body("revoked", is(now.toString("dd MMM YYYY")));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK);
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    public void respondWith404_whenRevokingTokenForAnotherAccount() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID_2, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK_2 + "\"}")
                .statusCode(404)
                .body("message", is("Could not revoke token"));

        Optional<String> token1RevokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK);
        assertThat(token1RevokedInDb.isPresent(), is(false));
        Optional<String> token2RevokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK_2);
        assertThat(token2RevokedInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith404_whenRevokingTokenAlreadyRevoked() throws Exception {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, DateTime.now(), CREATED_USER_NAME);

        revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK + "\"}")
                .statusCode(404)
                .body("message", is("Could not revoke token"));

        Optional<String> token1RevokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK);
        assertThat(token1RevokedInDb.isPresent(), is(true));
    }

    @Test
    public void respondWith404_whenTokenDoesNotExist() throws Exception {
        revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK + "\"}")
                .statusCode(404)
                .body("message", is("Could not revoke token"));

        Optional<String> tokenLinkdInDb = app.getDatabaseHelper().lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK);
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

        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);

        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Basic " + apiKey)
                .get(API_AUTH_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    public void shouldNotStoreTheTokenInThePlain() throws Exception {
        String newToken = createTokenFor(validTokenPayload)
                .statusCode(200)
                .extract()
                .body()
                .path("token");

        Optional<String> storedTokenHash = app.getDatabaseHelper().lookupColumnForTokenTable(TOKEN_HASH_COLUMN, "account_id", ACCOUNT_ID);
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

    private ValidatableResponse getTokensFor(String accountId, String tokenState) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .param("state", tokenState)
                .get(FRONTEND_AUTH_PATH + "/" + accountId)
                .then();
    }

    private ValidatableResponse getTokensForWithNoQueryParam(String accountId) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .get(FRONTEND_AUTH_PATH + "/" + accountId)
                .then();
    }

    private ValidatableResponse getTokensFor(String accountId) {
        return getTokensFor(accountId, "active");
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

    private Matcher<ReadableInstant> isCloseTo(DateTime now) {
        return both(greaterThan(now.minusSeconds(5))).and(lessThan(now.plusSeconds(5)));
    }

    private String encodedHmacValueOf(String input) {
        return BaseEncoding.base32Hex().lowerCase().omitPadding().encode(HmacUtils.hmacSha1("qwer9yuhgf", input));
    }
}
