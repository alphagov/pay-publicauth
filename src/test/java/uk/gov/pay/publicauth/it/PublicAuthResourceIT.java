package uk.gov.pay.publicauth.it;

import com.google.common.io.BaseEncoding;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import uk.gov.pay.publicauth.model.ServiceMode;
import uk.gov.pay.publicauth.model.TokenHash;
import uk.gov.pay.publicauth.model.TokenLink;
import uk.gov.pay.publicauth.model.TokenPaymentType;
import uk.gov.pay.publicauth.model.TokenSource;
import uk.gov.pay.publicauth.utils.DatabaseTestHelper;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresExtension;

import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.publicauth.model.TokenPaymentType.CARD;
import static uk.gov.pay.publicauth.model.TokenPaymentType.DIRECT_DEBIT;
import static uk.gov.pay.publicauth.model.TokenSource.API;
import static uk.gov.pay.publicauth.model.TokenSource.PRODUCTS;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AUTH_TOKEN_INVALID;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AUTH_TOKEN_REVOKED;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(DropwizardAppWithPostgresExtension.class)
class PublicAuthResourceIT {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy - HH:mm");
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
    private static final ServiceMode SERVICE_MODE = ServiceMode.TEST;
    private static final String SERVICE_EXTERNAL_ID = "cd1b871207a94a7fa157dee678146acd";
    private final Map<String, String> validTokenPayload = Map.of("account_id", ACCOUNT_ID,
                    "description", TOKEN_DESCRIPTION,
                    "token_account_type", "live",
                    "created_by", USER_EMAIL);
    private final Map<String, String> validDirectDebitTokenPayload = Map.of("account_id", ACCOUNT_ID,
                    "description", TOKEN_DESCRIPTION,
                    "token_type", DIRECT_DEBIT.toString(),
                    "created_by", USER_EMAIL);
    private final Map<String, String> validProductsTokenPayload = Map.of("account_id", ACCOUNT_ID,
                    "description", TOKEN_DESCRIPTION,
                    "type", PRODUCTS.toString(),
                    "created_by", USER_EMAIL);
    private final Map<String, String> validTokenPayloadWithTokenAccountType = Map.of("account_id", ACCOUNT_ID,
                    "token_account_type", "test",
                    "description", TOKEN_DESCRIPTION,
                    "created_by", USER_EMAIL);

    private DatabaseTestHelper databaseHelper;
    private Integer localPort;

    @BeforeEach
    public void setup(Integer port, DatabaseTestHelper databaseTestHelper) {
        databaseHelper = databaseTestHelper;
        localPort = port;
    }

    @Nested
    class TokenAuthentication {
        
        @Test
        void respondWith200_whenAuthWithValidToken() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
            String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);
            tokenResponse(apiKey)
                    .statusCode(200)
                    .body("account_id", is(ACCOUNT_ID))
                    .body("token_type", is(CARD.toString()))
                    .body("token_link", is(TOKEN_LINK.toString()));
            ZonedDateTime lastUsed = databaseHelper.getDateTimeColumn("last_used", ACCOUNT_ID);
            assertThat(lastUsed, isCloseTo(ZonedDateTime.now(UTC)));
        }

        @Test
        void respondWith200_serviceMode_serviceExternalId_whenAuthWithValidToken() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, TokenSource.API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, CREATED_USER_NAME, null, TokenPaymentType.CARD, SERVICE_MODE, SERVICE_EXTERNAL_ID);
            String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);
            tokenResponse(apiKey)
                    .statusCode(200)
                    .body("account_id", is(ACCOUNT_ID))
                    .body("token_type", is(CARD.toString()))
                    .body("token_link", is(TOKEN_LINK.toString()))
                    .body("service_mode", is(SERVICE_MODE.toString()))
                    .body("service_external_id", is(SERVICE_EXTERNAL_ID));
            ZonedDateTime lastUsed = databaseHelper.getDateTimeColumn("last_used", ACCOUNT_ID);
            assertThat(lastUsed, isCloseTo(ZonedDateTime.now(UTC)));
        }
    
        @Test
        void respondWith401_whenAuthWithRevokedToken() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION,
                    ZonedDateTime.now(UTC), CREATED_USER_NAME);
            String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);
            ZonedDateTime lastUsedPreAuth = databaseHelper.getDateTimeColumn("last_used", ACCOUNT_ID);
            tokenResponse(apiKey)
                    .statusCode(401)
                    .body("message", is(format("Token with token_link %s has been revoked", TOKEN_LINK)))
                    .body("error_identifier", is(AUTH_TOKEN_REVOKED.toString()))
                    .body("token_link", is(TOKEN_LINK.toString()));
            ZonedDateTime lastUsedPostAuth = databaseHelper.getDateTimeColumn("last_used", ACCOUNT_ID);
    
            assertThat(lastUsedPreAuth, is(lastUsedPostAuth));
        }
    
        @Test
        public void respondWith401_whenAuthWithNonExistentToken() {
            String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);
            tokenResponse(apiKey)
                    .statusCode(401)
                    .body("error_identifier", is(AUTH_TOKEN_INVALID.toString()));
        }

        @Test
        public void respondWith401_whenAuthHeaderIsMissing() {
            given().port(localPort)
                    .get(API_AUTH_PATH)
                    .then()
                    .statusCode(401);
        }

        @Test
        public void respondWith401_whenAuthHeaderIsBasicEvenWithValidToken() {

            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);

            String apiKey = BEARER_TOKEN + encodedHmacValueOf(BEARER_TOKEN);

            given().port(localPort)
                    .header(AUTHORIZATION, "Basic " + apiKey)
                    .get(API_AUTH_PATH)
                    .then()
                    .statusCode(401);
        }

        private ValidatableResponse tokenResponse(String token) {
            return given()
                    .port(localPort)
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
    
    @Nested
    class TokenCreation {
        
        @Test
        public void respondWith200_whenCreateAToken_ifProvidedBothAccountIdAndDescription() {
            String newToken = createTokenFor(validTokenPayload)
                    .statusCode(200)
                    .body("token", is(notNullValue()))
                    .extract().path("token");
    
            int apiKeyHashSize = 32;
            String tokenApiKey = newToken.substring(0, newToken.length() - apiKeyHashSize);
            String hashedToken = BCrypt.hashpw(tokenApiKey, SALT);
    
            Optional<String> newTokenDescription = databaseHelper.lookupColumnForTokenTable("description", TOKEN_HASH_COLUMN, hashedToken);
    
            assertThat(newTokenDescription.get(), equalTo(TOKEN_DESCRIPTION));
    
            Optional<String> newTokenAccountId = databaseHelper.lookupColumnForTokenTable("account_id", TOKEN_HASH_COLUMN, hashedToken);
            assertThat(newTokenAccountId.get(), equalTo(ACCOUNT_ID));
    
            Optional<String> newCreatedByEmail = databaseHelper.lookupColumnForTokenTable("created_by", TOKEN_HASH_COLUMN, hashedToken);
            assertThat(newCreatedByEmail.get(), equalTo(USER_EMAIL));
    
            Optional<String> newTokenType = databaseHelper.lookupColumnForTokenTable("token_type", TOKEN_HASH_COLUMN, hashedToken);
            assertThat(newTokenType.get(), equalTo(CARD.toString()));
        }
    
        @Test
        public void respondWith200_whenCreateAToken_ifValidTokenAccountTypeProvided() {
            String newToken = createTokenFor(validTokenPayloadWithTokenAccountType)
                    .statusCode(200)
                    .body("token", is(notNullValue()))
                    .extract().path("token");
            assertThat(newToken.contains("api_test_"), is(true));
        }
        @Test
        public void respondWith200_whenCreateAToken_ifValidServiceModeAndServiceExternalIdProvided() {
            String newToken = createTokenFor(Map.of(
                    "account_id", ACCOUNT_ID,
                    "description", TOKEN_DESCRIPTION,
                    "created_by", USER_EMAIL,
                    "service_mode", SERVICE_MODE.toString().toLowerCase(),
                    "service_external_id", SERVICE_EXTERNAL_ID
            ))
                    .statusCode(200)
                    .body("token", is(notNullValue()))
                    .extract().path("token");
            
            int apiKeyHashSize = 32;
            String tokenApiKey = newToken.substring(0, newToken.length() - apiKeyHashSize);
            String hashedToken = BCrypt.hashpw(tokenApiKey, SALT);

            Optional<String> newCreatedByEmail = databaseHelper.lookupColumnForTokenTable("service_mode", TOKEN_HASH_COLUMN, hashedToken);
            assertThat(newCreatedByEmail.get(), equalTo(SERVICE_MODE.toString()));

            Optional<String> newTokenType = databaseHelper.lookupColumnForTokenTable("service_external_id", TOKEN_HASH_COLUMN, hashedToken);
            assertThat(newTokenType.get(), equalTo(SERVICE_EXTERNAL_ID));
        }
    
        @Test
        public void respondWith400_whenCreateAToken_ifInvalidTokenTypeProvided() {
            createTokenFor(Map.of("account_id", ACCOUNT_ID,
                    "token_account_type", "not-an-account-type",
                    "description", TOKEN_DESCRIPTION,
                    "created_by", USER_EMAIL))
                    .statusCode(400);
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
    
            Optional<String> newTokenType = databaseHelper.lookupColumnForTokenTable("token_type", TOKEN_HASH_COLUMN, hashedToken);
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
    
            Optional<String> newType = databaseHelper.lookupColumnForTokenTable("type", TOKEN_HASH_COLUMN, hashedToken);
            assertThat(newType.get(), equalTo(PRODUCTS.toString()));
        }
    
        @Test
        public void respondWith422_ifAccountAndDescriptionAreMissing() {
            createTokenFor(Map.of())
                    .statusCode(422)
                    .body("errors.size()", is(3))
                    .body("errors", containsInAnyOrder(
                            "description must not be null",
                            "createdBy must not be null",
                            "accountId must not be null"));
        }
    
        @Test
        public void respondWith422_ifAccountIsMissing() {
            createTokenFor(Map.of("description", ACCOUNT_ID, "created_by", "some-user"))
                    .statusCode(422)
                    .body("errors", equalTo(List.of("accountId must not be null")));
        }
    
        @Test
        public void respondWith422_ifDescriptionIsMissing() {
            createTokenFor(Map.of("account_id", ACCOUNT_ID, "created_by", "some-user"))
                    .statusCode(422)
                    .body("errors", equalTo(List.of("description must not be null")));
        }
    
        @Test
        public void respondWith422_ifBodyIsMissing() {
            given().port(localPort)
                    .accept(JSON)
                    .contentType(JSON)
                    .body("")
                    .post(FRONTEND_AUTH_PATH)
                    .then().statusCode(422)
                    .body("errors", equalTo(List.of("The request body must not be null")));
        }

        @Test
        public void shouldNotStoreTheTokenInThePlain() {
            String newToken = createTokenFor(validTokenPayload)
                    .statusCode(200)
                    .extract()
                    .body()
                    .path("token");

            Optional<String> storedTokenHash = databaseHelper.lookupColumnForTokenTable(TOKEN_HASH_COLUMN, "account_id", ACCOUNT_ID);
            assertThat(storedTokenHash.get(), is(not(newToken)));
        }

        private ValidatableResponse createTokenFor(Map<String, String> body) {
            return given().port(localPort)
                    .accept(JSON)
                    .contentType(JSON)
                    .body(body)
                    .post(FRONTEND_AUTH_PATH)
                    .then();
        }
    }

    @Nested
    class TokenRetrieval {
        
        @Test
        public void respondWith200_andEmptyList_ifNoTokensHaveBeenIssuedForTheAccount() {
            getTokensFor(ACCOUNT_ID)
                    .statusCode(200)
                    .body("tokens", hasSize(0));
        }
        
        @Test
        void get_token_by_tokenLink_should_return_200_if_token_exists() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, null, CREATED_USER_NAME, null);

            given().port(localPort)
                    .accept(JSON)
                    .get(String.format("/v1/frontend/auth/%s/%s", ACCOUNT_ID, TOKEN_LINK))
                    .then()
                    .statusCode(200)
                    .body("token_link", is(TOKEN_LINK.toString()))
                    .body("description", is(TOKEN_DESCRIPTION))
                    .body("last_used", nullValue())
                    .body("created_by", is(CREATED_USER_NAME))
                    .body("token_type", is(CARD.toString()));
        }

        @Test
        void get_token_by_tokenLink_should_return_serviceMode_and_serviceExternalId() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, TokenSource.API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, CREATED_USER_NAME, null, TokenPaymentType.CARD, SERVICE_MODE, SERVICE_EXTERNAL_ID);
            
            given().port(localPort)
                    .accept(JSON)
                    .get(String.format("/v1/frontend/auth/%s/%s", ACCOUNT_ID, TOKEN_LINK))
                    .then()
                    .statusCode(200)
                    .body("token_link", is(TOKEN_LINK.toString()))
                    .body("description", is(TOKEN_DESCRIPTION))
                    .body("last_used", nullValue())
                    .body("created_by", is(CREATED_USER_NAME))
                    .body("token_type", is(CARD.toString()))
                    .body("service_mode", is(SERVICE_MODE.toString()))
                    .body("service_external_id", is(SERVICE_EXTERNAL_ID));
        }

        @Test
        void get_token_by_tokenLink_should_return_404_if_token_does_not_exist() {
            given().port(localPort)
                    .accept(JSON)
                    .get(String.format("/v1/frontend/auth/%s/123-456", ACCOUNT_ID))
                    .then()
                    .statusCode(404);
        }
    
        @Test
        public void respondWith200_ifTokensHaveBeenIssuedForTheAccount() {
            ZonedDateTime inserted = ZonedDateTime.now(UTC);
            ZonedDateTime lastUsed = inserted.plusHours(1);
    
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, null, CREATED_USER_NAME, lastUsed);
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, API, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed, DIRECT_DEBIT);
            databaseHelper.insertAccount(TokenHash.of("TOKEN-3"), TokenLink.of("123456789101112131415161718192021224"), PRODUCTS, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed, CARD);
    
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
            ZonedDateTime inserted = ZonedDateTime.now(UTC);
            ZonedDateTime lastUsed = inserted.plusHours(1);
            ZonedDateTime revoked = inserted.plusHours(2);
    
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, CREATED_USER_NAME, lastUsed);
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);
    
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
            ZonedDateTime inserted = ZonedDateTime.now(UTC);
            ZonedDateTime lastUsed = inserted.plusHours(1);
    
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, PRODUCTS, ACCOUNT_ID, TOKEN_DESCRIPTION, null, CREATED_USER_NAME, lastUsed, DIRECT_DEBIT);
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, PRODUCTS, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);
            databaseHelper.insertAccount(TokenHash.of("TOKEN-3"), TokenLink.of("123456789101112131415161718192021224"), API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, CREATED_USER_NAME2, lastUsed, CARD);
    
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
            ZonedDateTime inserted = ZonedDateTime.now(UTC);
            ZonedDateTime lastUsed = inserted.plusHours(1);
            ZonedDateTime revoked = inserted.plusHours(2);
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, CREATED_USER_NAME, lastUsed);
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);
    
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
            ZonedDateTime inserted = ZonedDateTime.now(UTC);
            ZonedDateTime lastUsed = inserted.plusHours(1);
            ZonedDateTime revoked = inserted.plusHours(2);
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, CREATED_USER_NAME, lastUsed);
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);
    
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
            ZonedDateTime inserted = ZonedDateTime.now(UTC);
            ZonedDateTime lastUsed = inserted.plusHours(1);
            ZonedDateTime revoked = inserted.plusHours(2);
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, CREATED_USER_NAME, lastUsed);
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, CREATED_USER_NAME2, lastUsed);
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

        private ValidatableResponse getTokensFor(String accountId) {
            return getTokensFor(accountId, "active");
        }

        private ValidatableResponse getTokensFor(String accountId, String tokenState) {
            return getTokensFor(accountId, tokenState, API.toString());
        }

        private ValidatableResponse getTokensFor(String accountId, String tokenState, String type) {
            return given().port(localPort)
                    .accept(JSON)
                    .param("state", tokenState)
                    .param("type", type)
                    .get(FRONTEND_AUTH_PATH + "/" + accountId)
                    .then();
        }

        private ValidatableResponse getTokensForWithNoQueryParam(String accountId) {
            return given().port(localPort)
                    .accept(JSON)
                    .get(FRONTEND_AUTH_PATH + "/" + accountId)
                    .then();
        }
    }

    @Nested
    class UpdateTokens {
        
        @Test
        public void respondWith400_ifNotProvidingDescription_whenUpdating() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
    
            updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK.toString() + "\"}")
                    .statusCode(400)
                    .body("message", is("Missing fields: [description]"));
    
            Optional<String> tokenLinkInDb = databaseHelper.lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK.toString());
            Optional<String> descriptionInDb = databaseHelper.lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
            assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
            assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK.toString()));
        }
    
        @Test
        public void respondWith400_ifNotProvidingTokenLink_whenUpdating() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
    
            updateTokenDescription("{\"description\" : \"" + TOKEN_DESCRIPTION + "\"}")
                    .statusCode(400)
                    .body("message", is("Missing fields: [token_link]"));
    
            Optional<String> tokenLinkInDb = databaseHelper.lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK.toString());
            Optional<String> descriptionInDb = databaseHelper.lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
            assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
            assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK.toString()));
        }
    
        @Test
        public void respondWith400_ifNotProvidingTokenLinkNorDescription_whenUpdating() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
    
            updateTokenDescription("{}")
                    .statusCode(400)
                    .body("message", is("Missing fields: [token_link, description]"));
    
            Optional<String> tokenLinkInDb = databaseHelper.lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK.toString());
            Optional<String> descriptionInDb = databaseHelper.lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
            assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
            assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK.toString()));
        }
    
        @Test
        public void respondWith400_ifNotProvidingBody_whenUpdating() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
    
            updateTokenDescription("")
                    .statusCode(400)
                    .body("message", is("Body cannot be empty"));
    
            Optional<String> tokenLinkInDb = databaseHelper.lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK.toString());
            Optional<String> descriptionInDb = databaseHelper.lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
            assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
            assertThat(tokenLinkInDb.get(), equalTo(TOKEN_LINK.toString()));
        }
    
        @Test
        public void respondWith200_ifUpdatingDescriptionOfExistingToken() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
            ZonedDateTime nowFromDB = ZonedDateTime.now(UTC);
    
            updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK.toString() + "\", \"description\" : \"" + TOKEN_DESCRIPTION_2 + "\"}")
                    .statusCode(200)
                    .body("token_link", is(TOKEN_LINK.toString()))
                    .body("description", is(TOKEN_DESCRIPTION_2))
                    .body("issued_date", is(nowFromDB.format(DATE_TIME_FORMAT)))
                    .body("last_used", is(nowFromDB.format(DATE_TIME_FORMAT)))
                    .body("created_by", is(CREATED_USER_NAME))
                    .body("token_type", is(CARD.toString()));
    
            Optional<String> descriptionInDb = databaseHelper.lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
            assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION_2));
        }
    
        @Test
        public void respondWith404_ifUpdatingDescriptionOfNonExistingToken() {
            updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK.toString() + "\", \"description\" : \"" + TOKEN_DESCRIPTION_2 + "\"}")
                    .statusCode(404)
                    .body("message", is("Could not update description of token with token_link " + TOKEN_LINK));
    
            Optional<String> descriptionInDb = databaseHelper.lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
            assertThat(descriptionInDb.isPresent(), is(false));
        }
    
        @Test
        public void respondWith404_butDoNotUpdateRevokedTokens() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, ZonedDateTime.now(), CREATED_USER_NAME);
    
            updateTokenDescription("{\"token_link\" : \"" + TOKEN_LINK.toString() + "\", \"description\" : \"" + TOKEN_DESCRIPTION_2 + "\"}")
                    .statusCode(404)
                    .body("message", is("Could not update description of token with token_link " + TOKEN_LINK));
    
            Optional<String> descriptionInDb = databaseHelper.lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
            assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
        }

        private ValidatableResponse updateTokenDescription(String body) {
            return given().port(localPort)
                    .accept(JSON)
                    .contentType(JSON)
                    .body(body)
                    .put(FRONTEND_AUTH_PATH)
                    .then();
        }
    }

    @Nested
    class TokenRevocation {
        
        @Test
        public void respondWith400_ifNotProvidingBody_whenRevokingAToken() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
    
            revokeSingleToken(ACCOUNT_ID, "")
                    .statusCode(400)
                    .body("message", is("Body cannot be empty"));
    
            Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
            assertThat(revokedInDb.isPresent(), is(false));
        }
    
        @Test
        public void respondWith400_ifProvidingEmptyBody_whenRevokingAToken() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
    
            revokeSingleToken(ACCOUNT_ID, "{}")
                    .statusCode(400)
                    .body("message", is("At least one of these fields must be present: [token_link, token]"));
    
            Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
            assertThat(revokedInDb.isPresent(), is(false));
        }
    
        @Test
        public void respondWith200_whenSingleTokenIsRevokedByTokenLink() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
    
            revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK + "\"}")
                    .statusCode(200)
                    .body("revoked", is(ZonedDateTime.now(UTC).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));
    
            Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
            assertThat(revokedInDb.isPresent(), is(true));
        }
    
        @Test
        public void respondWith200_whenSingleTokenIsRevokedByToken() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
            String fullBearerToken = BEARER_TOKEN + "qgs2ot3itqer7ag9mvvbs8snqb5jfas3";
            revokeSingleToken(ACCOUNT_ID, "{\"token\" : \"" + fullBearerToken + "\"}")
                    .statusCode(200)
                    .body("revoked", is(ZonedDateTime.now(UTC).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));
    
            Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
            assertThat(revokedInDb.isPresent(), is(true));
        }
    
        @Test
        public void respondWith404_whenRevokingTokenForAnotherAccount() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, CREATED_USER_NAME);
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN_2, TOKEN_LINK_2, ACCOUNT_ID_2, TOKEN_DESCRIPTION, CREATED_USER_NAME);
    
            revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK_2.toString() + "\"}")
                    .statusCode(404)
                    .body("message", is("Could not revoke token with token_link " + TOKEN_LINK_2));
    
            Optional<String> token1RevokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
            assertThat(token1RevokedInDb.isPresent(), is(false));
            Optional<String> token2RevokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK_2.toString());
            assertThat(token2RevokedInDb.isPresent(), is(false));
        }
    
        @Test
        public void respondWith404_whenRevokingTokenAlreadyRevoked() {
            databaseHelper.insertAccount(HASHED_BEARER_TOKEN, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, ZonedDateTime.now(), CREATED_USER_NAME);
    
            revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK.toString() + "\"}")
                    .statusCode(404)
                    .body("message", is("Could not revoke token with token_link " + TOKEN_LINK));
    
            Optional<String> token1RevokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
            assertThat(token1RevokedInDb.isPresent(), is(true));
        }
    
        @Test
        public void respondWith404_whenTokenDoesNotExist() {
            revokeSingleToken(ACCOUNT_ID, "{\"token_link\" : \"" + TOKEN_LINK.toString() + "\"}")
                    .statusCode(404)
                    .body("message", is("Could not revoke token with token_link " + TOKEN_LINK));
    
            Optional<String> tokenLinkdInDb = databaseHelper.lookupColumnForTokenTable("token_link", "token_link", TOKEN_LINK.toString());
            assertThat(tokenLinkdInDb.isPresent(), is(false));
        }

        private ValidatableResponse revokeSingleToken(String accountId, String body) {
            return given().port(localPort)
                    .accept(JSON)
                    .contentType(JSON)
                    .body(body)
                    .delete(FRONTEND_AUTH_PATH + "/" + accountId)
                    .then();
        }
    }
}
