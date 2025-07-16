package uk.gov.pay.publicauth.dao;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.publicauth.app.PublicAuthApp;
import uk.gov.pay.publicauth.model.CreateTokenRequest;
import uk.gov.pay.publicauth.model.ServiceMode;
import uk.gov.pay.publicauth.model.TokenEntity;
import uk.gov.pay.publicauth.model.TokenHash;
import uk.gov.pay.publicauth.model.TokenLink;
import uk.gov.pay.publicauth.utils.DatabaseTestHelper;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresExtension;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.pay.publicauth.model.TokenAccountType.LIVE;
import static uk.gov.pay.publicauth.model.TokenPaymentType.CARD;
import static uk.gov.pay.publicauth.model.TokenPaymentType.DIRECT_DEBIT;
import static uk.gov.pay.publicauth.model.TokenSource.API;
import static uk.gov.pay.publicauth.model.TokenSource.PRODUCTS;
import static uk.gov.pay.publicauth.model.TokenState.ACTIVE;
import static uk.gov.pay.publicauth.model.TokenState.REVOKED;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(DropwizardAppWithPostgresExtension.class)
class AuthTokenDaoIT {

    private static final String TEST_USER_NAME = "test-user-name";
    private static final String TEST_USER_NAME_2 = "test-user-name-2";
    private AuthTokenDao authTokenDao;
    private static final String ACCOUNT_ID = "564532435";
    private static final String ACCOUNT_ID_2 = "123456";
    private static final TokenHash TOKEN_HASH = TokenHash.of("TOKEN");
    private static final TokenHash TOKEN_HASH_2 = TokenHash.of("TOKEN-2");
    private static final TokenLink TOKEN_LINK = TokenLink.of("123456789101112131415161718192021222");
    private static final TokenLink TOKEN_LINK_2 = TokenLink.of("123456789101112131415161718192021223");
    private static final String TOKEN_DESCRIPTION = "Token description";
    private static final String TOKEN_DESCRIPTION_2 = "Token description 2";
    private static final String SERVICE_EXTERNAL_ID = "cd1b871207a94a7fa157dee678146acd";
    private DatabaseTestHelper databaseHelper;

    @BeforeEach
    public void setup(PublicAuthApp app, DatabaseTestHelper databaseTestHelper) {
        databaseHelper = databaseTestHelper;
        authTokenDao = new AuthTokenDao(app.getJdbi());
    }

    @Test
    void shouldFindATokenByHash() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, null, DIRECT_DEBIT, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        Optional<TokenEntity> tokenInfo = authTokenDao.findTokenByHash(TOKEN_HASH);
        assertThat(tokenInfo.get().getAccountId(), is(ACCOUNT_ID));
        assertThat(tokenInfo.get().getTokenPaymentType(), is(DIRECT_DEBIT));
    }

    @Test
    void shouldFindATokenByHash_returnsCardWhenPaymentTypeIsNull() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        Optional<TokenEntity> tokenInfo = authTokenDao.findTokenByHash(TOKEN_HASH);
        assertThat(tokenInfo.get().getAccountId(), is(ACCOUNT_ID));
        assertThat(tokenInfo.get().getTokenPaymentType(), is(CARD));
    }

    @Test
    void shouldReturnEmptyOptionalIfTokenWithHashIsNotFound() {
        Optional<TokenEntity> tokenInfo = authTokenDao.findTokenByHash(TOKEN_HASH);
        assertThat(tokenInfo, is(Optional.empty()));
    }

    @Test
    void shouldUpdateLastUsedTime() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, null, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        ZonedDateTime now = databaseHelper.getCurrentTime();

        Optional<TokenEntity> beforeUpdate = authTokenDao.findTokenByHash(TOKEN_HASH);
        assertThat(beforeUpdate.get().getLastUsedDate(), is(nullValue()));

        authTokenDao.updateLastUsedTime(TOKEN_HASH);

        Optional<TokenEntity> afterUpdate = authTokenDao.findTokenByHash(TOKEN_HASH);
        assertThat(afterUpdate.get().getLastUsedDate(), isCloseTo(now));
    }

    @Test
    void missingAccountHasNoAssociatedTokens() {
        List<TokenEntity> tokens = authTokenDao.findTokensBy(ACCOUNT_ID, ACTIVE, API);
        assertThat(tokens, is(Lists.newArrayList()));
    }

    @Test
    void shouldFindActiveApiTokens() {
        ZonedDateTime inserted = databaseHelper.getCurrentTime();
        ZonedDateTime lastUsed = inserted.plusMinutes(30);
        ZonedDateTime revoked = inserted.plusMinutes(45);
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, TEST_USER_NAME, lastUsed, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        databaseHelper.insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, API, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, lastUsed, DIRECT_DEBIT, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        databaseHelper.insertAccount(TokenHash.of("TOKEN-3"), TokenLink.of("123456789101112131415161718192021224"), PRODUCTS, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, lastUsed, DIRECT_DEBIT, ServiceMode.TEST, SERVICE_EXTERNAL_ID);

        List<TokenEntity> tokens = authTokenDao.findTokensBy(ACCOUNT_ID, ACTIVE, API);

        assertThat(tokens.size(), is(1));

        TokenEntity firstToken = tokens.getFirst();
        assertThat(firstToken.getTokenLink(), is(TOKEN_LINK_2));
        assertThat(firstToken.getDescription(), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.getRevokedDate(), is(nullValue()));
        assertThat(firstToken.getCreatedBy(), is(TEST_USER_NAME_2));
        assertThat(firstToken.getTokenPaymentType(), is(DIRECT_DEBIT));
        assertThat(firstToken.getLastUsedDate(), is(lastUsed));
        assertThat(firstToken.getIssuedDate(), isCloseTo(inserted));
    }

    @Test
    void shouldFindApiTokensByServiceInLiveMode() {
        ZonedDateTime inserted = databaseHelper.getCurrentTime();
        ZonedDateTime lastUsed = inserted.plusMinutes(30);
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, lastUsed, CARD, ServiceMode.LIVE, SERVICE_EXTERNAL_ID);
        databaseHelper.insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, API, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, lastUsed, CARD, ServiceMode.LIVE, SERVICE_EXTERNAL_ID);
        
        ZonedDateTime now = databaseHelper.getCurrentTime();

        List<TokenEntity> tokens = authTokenDao.findTokensBy(SERVICE_EXTERNAL_ID, ServiceMode.LIVE, ACTIVE, API);

        assertThat(tokens.size(), is(2));

        ZonedDateTime tokenIssueTime = databaseHelper.issueTimestampForAccount(ACCOUNT_ID);
        assertThat(tokenIssueTime, isCloseTo(now));
    }

    @Test
    void shouldFindApiTokensByServiceInTestMode() {
        ZonedDateTime inserted = databaseHelper.getCurrentTime();
        ZonedDateTime lastUsed = inserted.plusMinutes(30);
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, lastUsed, CARD, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        databaseHelper.insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, API, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, lastUsed, CARD, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        
        ZonedDateTime now = databaseHelper.getCurrentTime();

        List<TokenEntity> tokens = authTokenDao.findTokensBy(SERVICE_EXTERNAL_ID, ServiceMode.TEST, ACTIVE, API);

        assertThat(tokens.size(), is(2));

        ZonedDateTime tokenIssueTime = databaseHelper.issueTimestampForAccount(ACCOUNT_ID);
        assertThat(tokenIssueTime, isCloseTo(now));
    }

    @Test
    void shouldReturnCardTokensIfTokenPaymentTypeIsNull() {
        ZonedDateTime inserted = databaseHelper.getCurrentTime();
        ZonedDateTime lastUsed = inserted.plusMinutes(30);
        databaseHelper.insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, API, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, lastUsed, ServiceMode.TEST, SERVICE_EXTERNAL_ID);

        List<TokenEntity> tokens = authTokenDao.findTokensBy(ACCOUNT_ID, ACTIVE, API);

        assertThat(tokens.size(), is(1));

        TokenEntity firstToken = tokens.getFirst();
        assertThat(firstToken.getTokenLink(), is(TOKEN_LINK_2));
        assertThat(firstToken.getDescription(), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.getTokenSource(), is(API));
        assertThat(firstToken.getRevokedDate(), is(nullValue()));
        assertThat(firstToken.getCreatedBy(), is(TEST_USER_NAME_2));
        assertThat(firstToken.getTokenPaymentType(), is(CARD));
        assertThat(firstToken.getLastUsedDate(), is(lastUsed));
        assertThat(firstToken.getIssuedDate(), isCloseTo(inserted));
    }

    @Test
    void shouldFindRevokedApiTokens() {
        ZonedDateTime inserted = databaseHelper.getCurrentTime();
        ZonedDateTime lastUsed = inserted.plusMinutes(30);
        ZonedDateTime revoked = inserted.plusMinutes(45);
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, TEST_USER_NAME, lastUsed, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        databaseHelper.insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, API, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, lastUsed, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        databaseHelper.insertAccount(TokenHash.of("TOKEN-3"), TokenLink.of("123456789101112131415161718192021224"), PRODUCTS, ACCOUNT_ID, TOKEN_DESCRIPTION, revoked, TEST_USER_NAME, lastUsed, ServiceMode.TEST, SERVICE_EXTERNAL_ID);

        List<TokenEntity> tokens = authTokenDao.findTokensBy(ACCOUNT_ID, REVOKED, API);

        assertThat(tokens.size(), is(1));
        TokenEntity firstToken = tokens.getFirst();
        assertThat(firstToken.getTokenLink(), is(TOKEN_LINK));
        assertThat(firstToken.getDescription(), is(TOKEN_DESCRIPTION));
        assertThat(firstToken.getTokenSource(), is(API));
        assertThat(firstToken.getRevokedDate(), is(revoked));
        assertThat(firstToken.getCreatedBy(), is(TEST_USER_NAME));
        assertThat(firstToken.getLastUsedDate(), is(lastUsed));
        assertThat(firstToken.getIssuedDate(), isCloseTo(inserted));
    }

    @Test
    void shouldInsertNewToken() {
        var createTokenRequest = new CreateTokenRequest("account-id", "description", "user", CARD, API, LIVE, ServiceMode.LIVE, SERVICE_EXTERNAL_ID);
        authTokenDao.storeToken(TokenHash.of("token-hash"), createTokenRequest);
        Map<String, Object> tokenByHash = databaseHelper.getTokenByHash(TokenHash.of("token-hash"));
        ZonedDateTime now = databaseHelper.getCurrentTime();

        assertThat(tokenByHash.get("token_hash"), is("token-hash"));
        assertThat(tokenByHash.get("type"), is(API.toString()));
        assertThat(tokenByHash.get("account_id"), is("account-id"));
        assertThat(tokenByHash.get("description"), is("description"));
        assertThat(tokenByHash.get("created_by"), is("user"));
        assertNull(tokenByHash.get("last_used"));
        assertThat(tokenByHash.get("token_type"), is(CARD.toString()));
        assertThat(tokenByHash.get("service_mode"), is(ServiceMode.LIVE.toString()));
        assertThat(tokenByHash.get("service_external_id"), is(SERVICE_EXTERNAL_ID));
        ZonedDateTime tokenIssueTime = databaseHelper.issueTimestampForAccount("account-id");
        assertThat(tokenIssueTime, isCloseTo(now));
    }

    @Test
    void updateAnExistingToken() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, TEST_USER_NAME, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        boolean updateResult = authTokenDao.updateTokenDescription(TOKEN_LINK, TOKEN_DESCRIPTION_2);

        assertThat(updateResult, is(true));
        Optional<String> descriptionInDb = databaseHelper.lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION_2));
    }

    @Test
    void shouldFindTokenByTokenLink() {
        ZonedDateTime now = databaseHelper.getCurrentTime();
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, PRODUCTS, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, now, DIRECT_DEBIT, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        Optional<TokenEntity> tokenMayBe = authTokenDao.findTokenByTokenLink(TOKEN_LINK);
        TokenEntity token = tokenMayBe.get();

        assertThat(TOKEN_LINK, is(token.getTokenLink()));
        assertThat(TOKEN_DESCRIPTION, is(token.getDescription()));
        assertThat(TEST_USER_NAME, is(token.getCreatedBy()));
        assertThat(token.getTokenPaymentType(), is(DIRECT_DEBIT));
        assertThat(token.getTokenSource(), is(PRODUCTS));
        assertThat(token.getRevokedDate(), is(nullValue()));
        assertThat(token.getIssuedDate(), isCloseTo(now));
        assertThat(token.getLastUsedDate(), isCloseTo(now));
    }

    @Test
    void shouldFindTokenByAccountAndTokenLink() {
        ZonedDateTime now = databaseHelper.getCurrentTime();
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, PRODUCTS, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, now, CARD, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        Optional<TokenEntity> tokenMayBe = authTokenDao.findTokenBy(ACCOUNT_ID, TOKEN_LINK);
        TokenEntity token = tokenMayBe.get();

        assertThat(TOKEN_LINK, is(token.getTokenLink()));
        assertThat(TOKEN_DESCRIPTION, is(token.getDescription()));
        assertThat(TEST_USER_NAME, is(token.getCreatedBy()));
        assertThat(token.getTokenPaymentType(), is(CARD));
        assertThat(token.getTokenSource(), is(PRODUCTS));
        assertThat(token.getRevokedDate(), is(nullValue()));
        assertThat(token.getIssuedDate(), isCloseTo(now));
        assertThat(token.getLastUsedDate(), isCloseTo(now));
    }

    @Test
    void shouldFindTokenByServiceAndTokenLinkInLiveMode() {
        ZonedDateTime inserted = databaseHelper.getCurrentTime();
        ZonedDateTime lastUsed = inserted.plusMinutes(30);
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, lastUsed, CARD, ServiceMode.LIVE, SERVICE_EXTERNAL_ID);
        Optional<TokenEntity> tokenMayBe = authTokenDao.findTokenBy(SERVICE_EXTERNAL_ID, ServiceMode.LIVE, TOKEN_LINK);
        TokenEntity token = tokenMayBe.get();

        assertThat(TOKEN_LINK, is(token.getTokenLink()));
        assertThat(TOKEN_DESCRIPTION, is(token.getDescription()));
        assertThat(TEST_USER_NAME, is(token.getCreatedBy()));
        assertThat(token.getTokenPaymentType(), is(CARD));
        assertThat(token.getTokenSource(), is(API));
        assertThat(token.getRevokedDate(), is(nullValue()));
        assertThat(token.getIssuedDate(), isCloseTo(inserted));
    }

    @Test
    void shouldFindTokenByServiceAndTokenLinkInTestMode() {
        ZonedDateTime inserted = databaseHelper.getCurrentTime();
        ZonedDateTime lastUsed = inserted.plusMinutes(30);
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, lastUsed, CARD, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        Optional<TokenEntity> tokenMayBe = authTokenDao.findTokenBy(SERVICE_EXTERNAL_ID, ServiceMode.TEST, TOKEN_LINK);
        TokenEntity token = tokenMayBe.get();

        assertThat(TOKEN_LINK, is(token.getTokenLink()));
        assertThat(TOKEN_DESCRIPTION, is(token.getDescription()));
        assertThat(TEST_USER_NAME, is(token.getCreatedBy()));
        assertThat(token.getTokenPaymentType(), is(CARD));
        assertThat(token.getTokenSource(), is(API));
        assertThat(token.getRevokedDate(), is(nullValue()));
        assertThat(token.getIssuedDate(), isCloseTo(inserted));
    }

    @Test
    void shouldFindByTokenLinkAndReturnCardTokensIfTokenPaymentTypeIsNull() {
        ZonedDateTime now = databaseHelper.getCurrentTime();
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, now, null, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        Optional<TokenEntity> tokenMayBe = authTokenDao.findTokenByTokenLink(TOKEN_LINK);
        TokenEntity token = tokenMayBe.get();
        assertThat(TOKEN_LINK, is(token.getTokenLink()));
        assertThat(TOKEN_DESCRIPTION, is(token.getDescription()));
        assertThat(TEST_USER_NAME, is(token.getCreatedBy()));
        assertThat(token.getTokenPaymentType(), is(CARD));
        assertThat(token.getRevokedDate(), is(nullValue()));
        assertThat(token.getIssuedDate(), isCloseTo(now));
        assertThat(token.getLastUsedDate(), isCloseTo(now));
    }

    @Test
    void notUpdateANonExistingToken() {
        boolean updateResult = authTokenDao.updateTokenDescription(TOKEN_LINK, TOKEN_DESCRIPTION_2);

        assertThat(updateResult, is(false));
        Optional<String> descriptionInDb = databaseHelper.lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
        assertThat(descriptionInDb.isPresent(), is(false));
    }

    @Test
    void notUpdateARevokedToken() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, ZonedDateTime.now(UTC), TEST_USER_NAME, ServiceMode.TEST, SERVICE_EXTERNAL_ID);

        boolean updateResult = authTokenDao.updateTokenDescription(TOKEN_LINK, TOKEN_DESCRIPTION_2);

        assertThat(updateResult, is(false));
        Optional<String> descriptionInDb = databaseHelper.lookupColumnForTokenTable("description", "token_link", TOKEN_LINK.toString());
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
    }

    @Test
    void shouldRevokeASingleTokenByTokenLink() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, TEST_USER_NAME, ServiceMode.TEST, SERVICE_EXTERNAL_ID);

        Optional<LocalDateTime> revokedDate = authTokenDao.revokeSingleToken(ACCOUNT_ID, TOKEN_LINK);

        var actual = revokedDate.get().atZone(UTC);

        assertThat(actual, isCloseTo(ZonedDateTime.now(UTC)));

        Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    void shouldRevokeASingleTokenByServiceAndTokenLinkInLiveMode() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, null, CARD, ServiceMode.LIVE, SERVICE_EXTERNAL_ID);

        Optional<LocalDateTime> revokedDate = authTokenDao.revokeSingleToken(SERVICE_EXTERNAL_ID, ServiceMode.LIVE, TOKEN_LINK);

        var actual = revokedDate.get().atZone(UTC);

        assertThat(actual, isCloseTo(ZonedDateTime.now(UTC)));

        Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    void shouldRevokeASingleTokenByServiceAndTokenLinkInTestMode() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, null, CARD, ServiceMode.TEST, SERVICE_EXTERNAL_ID);

        Optional<LocalDateTime> revokedDate = authTokenDao.revokeSingleToken(SERVICE_EXTERNAL_ID, ServiceMode.TEST, TOKEN_LINK);

        var actual = revokedDate.get().atZone(UTC);

        assertThat(actual, isCloseTo(ZonedDateTime.now(UTC)));

        Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(revokedInDb.isPresent(), is(true));
    }
    
    @Test
    void shouldRevokeASingleTokenByTokenHash() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, TEST_USER_NAME, ServiceMode.TEST, SERVICE_EXTERNAL_ID);

        Optional<LocalDateTime> revokedDate = authTokenDao.revokeSingleToken(ACCOUNT_ID, TOKEN_HASH);

        var actual = revokedDate.get().atZone(UTC);

        assertThat(actual, isCloseTo(ZonedDateTime.now(UTC)));

        Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    void shouldRevokeASingleTokenByServiceAndTokenHashInLiveMode() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, null, CARD, ServiceMode.LIVE, SERVICE_EXTERNAL_ID);

        Optional<LocalDateTime> revokedDate = authTokenDao.revokeSingleToken(SERVICE_EXTERNAL_ID, ServiceMode.LIVE, TOKEN_HASH);

        var actual = revokedDate.get().atZone(UTC);

        assertThat(actual, isCloseTo(ZonedDateTime.now(UTC)));

        Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    void shouldRevokeASingleTokenByServiceAndTokenHashInTestMode() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, null, CARD, ServiceMode.TEST, SERVICE_EXTERNAL_ID);

        Optional<LocalDateTime> revokedDate = authTokenDao.revokeSingleToken(SERVICE_EXTERNAL_ID, ServiceMode.TEST, TOKEN_LINK);

        var actual = revokedDate.get().atZone(UTC);

        assertThat(actual, isCloseTo(ZonedDateTime.now(UTC)));

        Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    void shouldNotRevokeATokenForAnotherAccount() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, TEST_USER_NAME, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        databaseHelper.insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, ACCOUNT_ID_2, TOKEN_DESCRIPTION_2, TEST_USER_NAME, ServiceMode.TEST, SERVICE_EXTERNAL_ID);

        Optional<LocalDateTime> revokedDate  = authTokenDao.revokeSingleToken(ACCOUNT_ID, TOKEN_LINK_2);

        assertThat(revokedDate.isPresent(), is(false));

        Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(revokedInDb.isPresent(), is(false));
    }

    @Test
    void shouldNotRevokeATokenAlreadyRevoked() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION,  ZonedDateTime.now(UTC), TEST_USER_NAME, ServiceMode.TEST, SERVICE_EXTERNAL_ID);

        Optional<LocalDateTime> revokedDate = authTokenDao.revokeSingleToken(ACCOUNT_ID, TOKEN_LINK);

        assertThat(revokedDate.isPresent(), is(false));

        Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    void shouldErrorIfTriesToSaveTheSameTokenTwice() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, TEST_USER_NAME, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        var createTokenRequest = new CreateTokenRequest(ACCOUNT_ID, TOKEN_DESCRIPTION, "test@email.com", CARD, API, LIVE, ServiceMode.LIVE, SERVICE_EXTERNAL_ID);
        Assertions.assertThrows(RuntimeException.class, () -> {
            authTokenDao.storeToken(TOKEN_HASH, createTokenRequest);
        });
    }
    
    @Test
    void shouldRevokeTokensByAccountId() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, TEST_USER_NAME, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        databaseHelper.insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2, TEST_USER_NAME, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        
        var revokedTokensCount = authTokenDao.revokeTokens(ACCOUNT_ID);
        
        Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        Optional<String> revokedInDb2 = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK_2.toString());
        
        assertThat(revokedInDb.isPresent(), is(true));
        assertThat(revokedInDb2.isPresent(), is(true));
        assertThat(revokedTokensCount, is(2));
    }

    @Test
    void shouldRevokeTokensByServiceInLiveMode() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, null, CARD, ServiceMode.LIVE, SERVICE_EXTERNAL_ID);
        databaseHelper.insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, API, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, null, CARD, ServiceMode.LIVE, SERVICE_EXTERNAL_ID);
        
        var revokedTokensCount = authTokenDao.revokeTokens(SERVICE_EXTERNAL_ID, ServiceMode.LIVE);

        Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        Optional<String> revokedInDb2 = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK_2.toString());
       
        assertThat(revokedInDb.isPresent(), is(true));
        assertThat(revokedInDb2.isPresent(), is(true));
        assertThat(revokedTokensCount, is(2));
    }

    @Test
    void shouldRevokeTokensByServiceInTestMode() {
        databaseHelper.insertAccount(TOKEN_HASH, TOKEN_LINK, API, ACCOUNT_ID, TOKEN_DESCRIPTION, null, TEST_USER_NAME, null, CARD, ServiceMode.TEST, SERVICE_EXTERNAL_ID);
        databaseHelper.insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, API, ACCOUNT_ID, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, null, CARD, ServiceMode.TEST, SERVICE_EXTERNAL_ID);

        var revokedTokensCount = authTokenDao.revokeTokens(SERVICE_EXTERNAL_ID, ServiceMode.TEST);

        Optional<String> revokedInDb = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK.toString());
        Optional<String> revokedInDb2 = databaseHelper.lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK_2.toString());

        assertThat(revokedInDb.isPresent(), is(true));
        assertThat(revokedInDb2.isPresent(), is(true));
        assertThat(revokedTokensCount, is(2));
    }

    private Matcher<ChronoZonedDateTime<?>> isCloseTo(ZonedDateTime now) {
        return both(greaterThan(now.minusSeconds(5))).and(lessThan(now.plusSeconds(5)));
    }
}
