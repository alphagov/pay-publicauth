package uk.gov.pay.publicauth.it;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;
import uk.gov.pay.publicauth.model.TokenHash;
import uk.gov.pay.publicauth.model.TokenLink;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresRule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.publicauth.model.TokenPaymentType.CARD;
import static uk.gov.pay.publicauth.model.TokenPaymentType.DIRECT_DEBIT;
import static uk.gov.pay.publicauth.model.TokenSource.API;
import static uk.gov.pay.publicauth.model.TokenSource.PRODUCTS;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class PublicAuthResourceIT {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM YYYY - HH:mm");

    private static final String SALT = "$2a$10$IhaXo6LIBhKIWOiGpbtPOu";
    private static final String BEARER_TOKEN = "testbearertoken";
    private static final TokenLink TOKEN_LINK = TokenLink.of("123456789101112131415161718192021222");
    private static final TokenLink TOKEN_LINK_2 = TokenLink.of("123456789101112131415161718192021223");
    private static final TokenHash HASHED_BEARER_TOKEN = TokenHash.of(BCrypt.hashpw(BEARER_TOKEN, SALT));
    private static final TokenHash HASHED_BEARER_TOKEN_2 = TokenHash.of(BCrypt.hashpw(BEARER_TOKEN + "2", SALT));
    private static final String API_AUTH_PATH = "/v1/api/auth";
    private static final String FRONTEND_AUTH_PATH = "/v1/frontend/auth";
    private static final String ACCOUNT_ID = "ACCOUNT-ID";
    private static final String ACCOUNT_ID_2 = "ACCOUNT-ID-2";
    private static final String TOKEN_DESCRIPTION = "TOKEN DESCRIPTION";
    private static final String TOKEN_DESCRIPTION_2 = "Token description 2";
    private static final String USER_EMAIL = "user@email.com";
    private static final String TOKEN_HASH_COLUMN = "token_hash";
    private static final String CREATED_USER_NAME = "user-name";
    private static final String CREATED_USER_NAME2 = "user-name-2";

    @Rule
    public final DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();
    private final String validTokenPayload = new Gson().toJson(
            ImmutableMap.of("account_id", ACCOUNT_ID,
                    "description", TOKEN_DESCRIPTION,
                    "created_by", USER_EMAIL));
    private final String validDirectDebitTokenPayload = new Gson().toJson(
            ImmutableMap.of("account_id", ACCOUNT_ID,
                    "description", TOKEN_DESCRIPTION,
                    "token_type", DIRECT_DEBIT.toString(),
                    "created_by", USER_EMAIL));
    private final String validProductsTokenPayload = new Gson().toJson(
            ImmutableMap.of("account_id", ACCOUNT_ID,
                    "description", TOKEN_DESCRIPTION,
                    "type", PRODUCTS.toString(),
                    "created_by", USER_EMAIL));
    @Test
    public void respondWith200_whenAuthWithValidToken() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
        String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);
        tokenResponse(apiKey)
                .statusCode(200)
                .body("account_id", is(ACCOUNT_ID));
        ZonedDateTime lastUsed = app.getDatabaseHelper().getDateTimeColumn("last_used", ACCOUNT_ID);
        assertThat(lastUsed, isCloseTo(ZonedDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    public void respondWith401_whenAuthWithRevokedToken() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION,
                ZonedDateTime.now(ZoneOffset.UTC), CREATED_USER_NAME);
        String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);
        ZonedDateTime lastUsedPreAuth = app.getDatabaseHelper().getDateTimeColumn("last_used", ACCOUNT_ID);
        tokenResponse(apiKey)
                .statusCode(401);
        ZonedDateTime lastUsedPostAuth = app.getDatabaseHelper().getDateTimeColumn("last_used", ACCOUNT_ID);

        assertThat(lastUsedPreAuth, is(lastUsedPostAuth));
    }

    @Test
    public void respondWith401_whenAuthWithNonExistentToken() {
        String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);
        tokenResponse(apiKey)
                .statusCode(401);
    }

    @Test
    public void respondWith200_whenCreateAToken_ifProvidedBothAccountIdAndDescription() {
        String newToken = createTokenFor(validTokenPayload)
                .statusCode(200)
                .body("token", is(notNullValue()))
                .extract().path("token");

        int apiKeyHashSize = 32;
        String tokenApiKey = newToken.substring(0, newToken.length() - apiKeyHashSize);
        String hashedToken = BCrypt.hashpw(tokenApiKey, SALT);

        Optional<String> newTokenDescription = app.getDatabaseHelper().lookupColumnForTokenTable("description", TOKEN_HASH_COLUMN, hashedToken);

        assertThat(newTokenDescription.get(), equalTo(TOKEN_DESCRIPTION));

        Optional<String> newTokenAccountId = app.getDatabaseHelper().lookupColumnForTokenTable("account_id", TOKEN_HASH_COLUMN, hashedToken);
        assertThat(newTokenAccountId.get(), equalTo(ACCOUNT_ID));

        Optional<String> newCreatedByEmail = app.getDatabaseHelper().lookupColumnForTokenTable("created_by", TOKEN_HASH_COLUMN, hashedToken);
        assertThat(newCreatedByEmail.get(), equalTo(USER_EMAIL));

        Optional<String> newTokenType = app.getDatabaseHelper().lookupColumnForTokenTable("token_type", TOKEN_HASH_COLUMN, hashedToken);
        assertThat(newTokenType.get(), equalTo(CARD.toString()));
    }

    @Test
    public void respondWith200_whenCreateAToken_ifProvidedAccountIdDescriptionAndTokenType() {
        String newToken = createTokenFor(validDirectDebitTokenPayload)
                .statusCode(200)
                .body("token", is(notNullValue()))
                .extract().path("token");

        int apiKeyHashSize = 32;
        String tokenApiKey = newToken.substring(0, newToken.length() - apiKeyHashSize);
        String hashedToken = BCrypt.hashpw(tokenApiKey, SALT);

        Optional<String> newTokenType = app.getDatabaseHelper().lookupColumnForTokenTable("token_type", TOKEN_HASH_COLUMN, hashedToken);
        assertThat(newTokenType.get(), equalTo(DIRECT_DEBIT.toString()));
    }

    @Test
    public void respondWith200_whenCreateAToken_ifProvidedAccountIdDescriptionAndType() {
        String newToken = createTokenFor(validProductsTokenPayload)
                .statusCode(200)
                .body("token", is(notNullValue()))
                .extract().path("token");

        int apiKeyHashSize = 32;
        String tokenApiKey = newToken.substring(0, newToken.length() - apiKeyHashSize);
        String hashedToken = BCrypt.hashpw(tokenApiKey, SALT);

        Optional<String> newType = app.getDatabaseHelper().lookupColumnForTokenTable("type", TOKEN_HASH_COLUMN, hashedToken);
        assertThat(newType.get(), equalTo(PRODUCTS.toString()));
    }
    
    @Test
    public void respondWith422_ifAccountAndDescriptionAreMissing() {
        createTokenFor("{}")
            .statusCode(422)
            .body("errors.size()", is(3))
            .body("errors", containsInAnyOrder(
                "description must not be null",
                "createdBy must not be null",
                "accountId must not be null"));
    }

    @Test
    public void respondWith422_ifAccountIsMissing() {
        createTokenFor("{\"description\" : \"" + ACCOUNT_ID + "\", \"created_by\": \"some-user\"}")
            .statusCode(422)
            .body("errors", equalTo(List.of("accountId must not be null")));
    }

    @Test
    public void respondWith422_ifDescriptionIsMissing() {
        createTokenFor("{\"account_id\" : \"" + ACCOUNT_ID + "\", \"created_by\": \"some-user\"}")
            .statusCode(422)
            .body("errors", equalTo(List.of("description must not be null")));
    }

    @Test
    public void respondWith422_ifBodyIsMissing() {
        createTokenFor("")
            .statusCode(422)
            .body("errors", equalTo(List.of("The request body must not be null")));
    }

    @Test
    public void respondWith200_andEmptyList_ifNoTokensHaveBeenIssuedForTheAccount() {
        getTokensFor(ACCOUNT_ID)
                .statusCode(200)
                .body("tokens", hasSize(0));
    }

    @Test
    public void respondWith200_ifTokensHaveBeenIssuedForTheAccount() {
        ZonedDateTime inserted = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime lastUsed = inserted.plusHours(1);

        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, null, CREATED_USER_NAME, lastUsed);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, API, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed, DIRECT_DEBIT);
        app.getDatabaseHelper().insertAccount(TokenHash.of("TOKEN-3"), TokenLink.of("123456789101112131415161718192021224"), PRODUCTS, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed, CARD);

        List<Map<String, String>> retrievedTokens = getTokensFor(ACCOUNT_ID)
                .statusCode(200)
                .body("tokens", hasSize(2))
                .extract().path("tokens");


        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.size(), is(7));
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2.toString()));
        assertThat(firstToken.get("type"), is(API.toString()));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));
        assertThat(firstToken.get("created_by"), is(CREATED_USER_NAME2));
        assertThat(firstToken.get("token_type"), is(DIRECT_DEBIT.toString()));
        assertThat(firstToken.get("issued_date"), is(inserted.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));

        Map<String, String> secondToken = retrievedTokens.get(1);
        assertThat(secondToken.size(), is(7));
        assertThat(secondToken.get("token_link"), is(TOKEN_LINK.toString()));
        assertThat(firstToken.get("type"), is(API.toString()));
        assertThat(secondToken.get("description"), is(TOKEN_DESCRIPTION));
        assertThat(secondToken.containsKey("revoked"), is(false));
        assertThat(secondToken.get("created_by"), is(CREATED_USER_NAME));
        assertThat(secondToken.get("token_type"), is(CARD.toString()));
        assertThat(secondToken.get("issued_date"), is(inserted.format(DATE_TIME_FORMAT)));
        assertThat(secondToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));
    }

    @Test
    public void respondWith200_andRetrieveRevokedTokens() {
        ZonedDateTime inserted = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime lastUsed = inserted.plusHours(1);
        ZonedDateTime revoked = inserted.plusHours(2);

        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, CREATED_USER_NAME, lastUsed);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);

        List<Map<String, String>> retrievedTokens = getTokensFor(ACCOUNT_ID, "revoked")
                .statusCode(200)
                .body("tokens", hasSize(1))
                .extract().path("tokens");


        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK.toString()));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION));
        assertThat(firstToken.get("revoked"), is(revoked.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("created_by"), is(CREATED_USER_NAME));
        assertThat(firstToken.get("token_type"), is(CARD.toString()));
        assertThat(firstToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("issued_date"), is(inserted.format(DATE_TIME_FORMAT)));
    }

    @Test
    public void respondWith200_andRetrieveProductsTokens() {
        ZonedDateTime inserted = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime lastUsed = inserted.plusHours(1);

        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, PRODUCTS, ACCOUNT_ID, TOKEN_DESCRIPTION, null, CREATED_USER_NAME, lastUsed, DIRECT_DEBIT);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, PRODUCTS, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);
        app.getDatabaseHelper().insertAccount(TokenHash.of("TOKEN-3"), TokenLink.of("123456789101112131415161718192021224"), API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, CREATED_USER_NAME2, lastUsed, CARD);

        List<Map<String, String>> retrievedTokens = getTokensFor(ACCOUNT_ID, "active", "products")
                .statusCode(200)
                .body("tokens", hasSize(2))
                .extract().path("tokens");


        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2.toString()));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));
        assertThat(firstToken.get("created_by"), is(CREATED_USER_NAME2));
        assertThat(firstToken.get("token_type"), is(CARD.toString()));
        assertThat(firstToken.get("type"), is(PRODUCTS.toString()));
        assertThat(firstToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));
        Map<String, String> secondToken = retrievedTokens.get(1);
        assertThat(secondToken.get("token_link"), is(TOKEN_LINK.toString()));
        assertThat(secondToken.get("description"), is(TOKEN_DESCRIPTION));
        assertThat(secondToken.containsKey("revoked"), is(false));
        assertThat(secondToken.get("created_by"), is(CREATED_USER_NAME));
        assertThat(secondToken.get("token_type"), is(DIRECT_DEBIT.toString()));
        assertThat(secondToken.get("type"), is(PRODUCTS.toString()));
        assertThat(secondToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));
    }

    @Test
    public void respondWith200_andRetrieveActiveTokens() {
        ZonedDateTime inserted = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime lastUsed = inserted.plusHours(1);
        ZonedDateTime revoked = inserted.plusHours(2);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, CREATED_USER_NAME, lastUsed);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);

        List<Map<String, String>> retrievedTokens = getTokensFor(ACCOUNT_ID, "active")
                .statusCode(200)
                .body("tokens", hasSize(1))
                .extract().path("tokens");

        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2.toString()));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));
        assertThat(firstToken.get("created_by"), is(CREATED_USER_NAME2));
        assertThat(firstToken.get("token_type"), is(CARD.toString()));
        assertThat(firstToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("issued_date"), is(inserted.format(DATE_TIME_FORMAT)));
    }

    @Test
    public void respondWith200_andRetrieveActiveTokensIfNoQueryParamIsSpecified() {
        ZonedDateTime inserted = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime lastUsed = inserted.plusHours(1);
        ZonedDateTime revoked = inserted.plusHours(2);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, CREATED_USER_NAME, lastUsed);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);

        List<Map<String, String>> retrievedTokens = getTokensForWithNoQueryParam(ACCOUNT_ID)
                .statusCode(200)
                .body("tokens", hasSize(1))
                .extract().path("tokens");

        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2.toString()));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));
        assertThat(firstToken.get("created_by"), is(CREATED_USER_NAME2));
        assertThat(firstToken.get("token_type"), is(CARD.toString()));
        assertThat(firstToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("issued_date"), is(inserted.format(DATE_TIME_FORMAT)));
    }

    @Test
    public void respondWith200_andRetrieveActiveTokensIfUnknownQueryParamIsSpecified() {
        ZonedDateTime inserted = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime lastUsed = inserted.plusHours(1);
        ZonedDateTime revoked = inserted.plusHours(2);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, CREATED_USER_NAME, lastUsed);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);
        List<Map<String, String>> retrievedTokens = getTokensFor(ACCOUNT_ID, "something")
                .statusCode(200)
                .body("tokens", hasSize(1))
                .extract().path("tokens");

        //Retrieved in issued order from newest to oldest
        Map<String, String> firstToken = retrievedTokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2.toString()));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));
        assertThat(firstToken.get("created_by"), is(CREATED_USER_NAME2));
        assertThat(firstToken.get("token_type"), is(CARD.toString()));
        assertThat(firstToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("issued_date"), is(inserted.format(DATE_TIME_FORMAT)));
    }

    @Test
    public void respondWith400_ifNotProvidingDescription_whenUpdating() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK.toString() + "\"}")
                .statusCode(400)
                .body("message", is("Missing fields: [description]"));

        Optional<String> tokenLinkInDb = app.getDatabaseHelper().lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK.toString());
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK.toString()));
    }

    @Test
    public void respondWith400_ifNotProvidingTokenLink_whenUpdating() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        updateTokenDescription("{\"description\" : \"" + TOKEN_DESCRIPTION + "\"}")
                .statusCode(400)
                .body("message", is("Missing fields: [token_link]"));

        Optional<String> tokenLinkInDb = app.getDatabaseHelper().lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK.toString());
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK.toString()));
    }

    @Test
    public void respondWith400_ifNotProvidingTokenLinkNorDescription_whenUpdating() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        updateTokenDescription("{}")
                .statusCode(400)
                .body("message", is("Missing fields: [token_link, description]"));

        Optional<String> tokenLinkInDb = app.getDatabaseHelper().lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK.toString());
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK.toString()));
    }

    @Test
    public void respondWith400_ifNotProvidingBody_whenUpdating() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        updateTokenDescription("")
                .statusCode(400)
                .body("message", is("Body cannot be empty"));

        Optional<String> tokenLinkInDb = app.getDatabaseHelper().lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK.toString());
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK.toString()));
    }

    @Test
    public void respondWith200_ifUpdatingDescriptionOfExistingToken() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
        ZonedDateTime nowFromDB = ZonedDateTime.now(ZoneOffset.UTC);

        updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK.toString() + "\", \"description\" : \"" + TOKEN_DESCRIPTION_2 + "\"}")
                .statusCode(200)
                .body("token_link", is(TOKEN_LINK.toString()))
                .body("description", is(TOKEN_DESCRIPTION_2))
                .body("issued_date", is(nowFromDB.format(DATE_TIME_FORMAT)))
                .body("last_used", is(nowFromDB.format(DATE_TIME_FORMAT)))
                .body("created_by", is(CREATED_USER_NAME))
                .body("token_type", is(CARD.toString()));

        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION_2));
    }

    @Test
    public void respondWith404_ifUpdatingDescriptionOfNonExistingToken() {
        updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK.toString() + "\", \"description\" : \"" + TOKEN_DESCRIPTION_2 + "\"}")
                .statusCode(404)
                .body("message", is("Could not update description of token with token_link " + TOKEN_LINK));

        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
        assertThat(descriptionInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith404_butDoNotUpdateRevokedTokens() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, ZonedDateTime.now(), CREATED_USER_NAME);

        updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK.toString() + "\", \"description\" : \"" + TOKEN_DESCRIPTION_2 + "\"}")
                .statusCode(404)
                .body("message", is("Could not update description of token with token_link " + TOKEN_LINK));

        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
    }

    @Test
    public void respondWith400_ifNotProvidingBody_whenRevokingAToken() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        revokeSingleToken(ACCOUNT_ID, "")
                .statusCode(400)
                .body("message", is("Body cannot be empty"));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(revokedInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith400_ifProvidingEmptyBody_whenRevokingAToken() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        revokeSingleToken(ACCOUNT_ID, "{}")
                .statusCode(400)
                .body("message", is("At least one of these fields must be present: [token_link, token]"));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(revokedInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith200_whenSingleTokenIsRevokedByTokenLink() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK.toString() + "\"}")
                .statusCode(200)
                .body("revoked", is(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("dd MMM YYYY"))));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    public void respondWith200_whenSingleTokenIsRevokedByToken() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
        String fullBearerToken = BEARER_TOKEN + "qgs2ot3itqer7ag9mvvbs8snqb5jfas3";
        revokeSingleToken(ACCOUNT_ID, "{\"token\" : \"" + fullBearerToken + "\"}")
                .statusCode(200)
                .body("revoked", is(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("dd MMM YYYY"))));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    public void respondWith404_whenRevokingTokenForAnotherAccount() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID_2, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK_2.toString() + "\"}")
                .statusCode(404)
                .body("message", is("Could not revoke token with token_link " + TOKEN_LINK_2));

        Optional<String> token1RevokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(token1RevokedInDb.isPresent(), is(false));
        Optional<String> token2RevokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK_2.toString());
        assertThat(token2RevokedInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith404_whenRevokingTokenAlreadyRevoked() {
        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, ZonedDateTime.now(), CREATED_USER_NAME);

        revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK.toString() + "\"}")
                .statusCode(404)
                .body("message", is("Could not revoke token with token_link " + TOKEN_LINK ));

        Optional<String> token1RevokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(token1RevokedInDb.isPresent(), is(true));
    }

    @Test
    public void respondWith404_whenTokenDoesNotExist() {
        revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK.toString() + "\"}")
                .statusCode(404)
                .body("message", is("Could not revoke token with token_link " + TOKEN_LINK));

        Optional<String> tokenLinkdInDb = app.getDatabaseHelper().lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK.toString());
        assertThat(tokenLinkdInDb.isPresent(), is(false));
    }

    @Test
    public void respondWith401_whenAuthHeaderIsMissing() {
        given().port(app.getLocalPort())
                .get(API_AUTH_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    public void respondWith401_whenAuthHeaderIsBasicEvenWithValidToken() {

        app.getDatabaseHelper().insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

        String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);

        given().port(app.getLocalPort())
                .header(AUTHORIZATION, "Basic " + apiKey)
                .get(API_AUTH_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    public void shouldNotStoreTheTokenInThePlain() {
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
        return getTokensFor(accountId, tokenState, API.toString());
    }

    private ValidatableResponse getTokensFor(String accountId, String tokenState, String type) {
        return given().port(app.getLocalPort())
                .accept(JSON)
                .param("state", tokenState)
                .param("type", type)
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

    private Matcher<ChronoZonedDateTime<?>> isCloseTo(ZonedDateTime now) {
        return both(greaterThan(now.minusSeconds(5))).and(lessThan(now.plusSeconds(5)));
    }

    private String encodedHmacValueOf(String input) {
        return BaseEncoding.base32Hex().lowerCase().omitPadding().encode(new HmacUtils(HmacAlgorithms.HMAC_SHA_1, "qwer9yuhgf").hmac(input));
    }
}
