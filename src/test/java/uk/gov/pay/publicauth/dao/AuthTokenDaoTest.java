package uk.gov.pay.publicauth.dao;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresRule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static uk.gov.pay.publicauth.model.TokenPaymentType.CARD;
import static uk.gov.pay.publicauth.model.TokenPaymentType.DIRECT_DEBIT;
import static uk.gov.pay.publicauth.model.TokenStateFilterParam.ACTIVE;
import static uk.gov.pay.publicauth.model.TokenStateFilterParam.REVOKED;

public class AuthTokenDaoTest {

    public static final String TEST_USER_NAME = "test-user-name";
    public static final String TEST_USER_NAME_2 = "test-user-name-2";

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM YYYY - HH:mm");


    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private AuthTokenDao authTokenDao;
    private static final String ACCOUNT_ID = "564532435";
    private static final String ACCOUNT_ID_2 = "123456";
    private static final String TOKEN_HASH = "TOKEN";
    private static final String TOKEN_HASH_2 = "TOKEN-2";
    private static final String TOKEN_LINK = "123456789101112131415161718192021222";
    private static final String TOKEN_LINK_2 = "123456789101112131415161718192021223";
    private static final String TOKEN_DESCRIPTION = "Token description";
    private static final String TOKEN_DESCRIPTION_2 = "Token description 2";

    @Before
    public void setUp() throws Exception {
        authTokenDao = new AuthTokenDao(app.getJdbi());
    }

    @Test
    public void findAnAccountIdAndTokenTypeByToken_ifTokenTypeIsDirectDebit() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, null, TEST_USER_NAME, null, DIRECT_DEBIT);
        Optional<Map<String, Object>> tokenInfo = authTokenDao.findUnRevokedAccount(TOKEN_HASH);
        assertThat(tokenInfo.get().get("account_id"), is(ACCOUNT_ID));
        assertThat(tokenInfo.get().get("token_type"), is(DIRECT_DEBIT.toString()));
    }


    @Test
    public void findAnAccountIdAndTokenTypeByToken_ifTokenTypeIsCard() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, null, TEST_USER_NAME, null, CARD);
        Optional<Map<String, Object>> tokenInfo = authTokenDao.findUnRevokedAccount(TOKEN_HASH);
        assertThat(tokenInfo.get().get("account_id"), is(ACCOUNT_ID));
        assertThat(tokenInfo.get().get("token_type"), is(CARD.toString()));
    }

    @Test
    public void findAnAccountIdAndTokenTypeByToken_ifTokenTypeIsNull() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, null, TEST_USER_NAME, null, null);
        Optional<Map<String, Object>> tokenInfo = authTokenDao.findUnRevokedAccount(TOKEN_HASH);
        assertThat(tokenInfo.get().get("account_id"), is(ACCOUNT_ID));
        assertThat(tokenInfo.get().get("token_type"), is(CARD.toString()));
    }

    @Test
    public void missingTokenHasNoInfo() throws Exception {
        Optional<Map<String, Object>> tokenInfo = authTokenDao.findUnRevokedAccount(TOKEN_HASH);
        assertThat(tokenInfo, is(Optional.empty()));
    }

    @Test
    public void missingAccountHasNoAssociatedTokens() throws Exception {
        List<Map<String, Object>> tokens = authTokenDao.findTokensWithStateById(ACCOUNT_ID, ACTIVE);
        assertThat(tokens, is(Lists.newArrayList()));
    }

    @Test
    public void shouldFindActiveTokens() throws Exception {
        ZonedDateTime inserted = app.getDatabaseHelper().getCurrentTime();
        ZonedDateTime lastUsed = inserted.plusMinutes(30);
        ZonedDateTime revoked = inserted.plusMinutes(45);
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, revoked, TEST_USER_NAME, lastUsed);
        app.getDatabaseHelper().insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, ACCOUNT_ID, null, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, lastUsed);

        List<Map<String, Object>> tokens = authTokenDao.findTokensWithStateById(ACCOUNT_ID, ACTIVE);

        assertThat(tokens.size(), is(1));

        Map<String, Object> firstToken = tokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));
        assertThat(firstToken.get("created_by"), is(TEST_USER_NAME_2));
        assertThat(firstToken.get("token_type"), is(CARD.toString()));
        assertThat(firstToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("issued_date"), is(inserted.format(DATE_TIME_FORMAT)));
    }
    @Test
    public void shouldFindActiveTokensByExternalId() throws Exception {
        ZonedDateTime inserted = app.getDatabaseHelper().getCurrentTime();
        ZonedDateTime lastUsed = inserted.plusMinutes(30);
        ZonedDateTime revoked = inserted.plusMinutes(45);
        String accountExternalId = "aslkdnaslkd";
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, accountExternalId, TOKEN_DESCRIPTION, revoked, TEST_USER_NAME, lastUsed, DIRECT_DEBIT);
        app.getDatabaseHelper().insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, ACCOUNT_ID, accountExternalId, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, lastUsed, DIRECT_DEBIT);

        List<Map<String, Object>> tokens = authTokenDao.findTokensWithStateByExternalId(accountExternalId, ACTIVE);

        assertThat(tokens.size(), is(1));

        Map<String, Object> firstToken = tokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));
        assertThat(firstToken.get("created_by"), is(TEST_USER_NAME_2));
        assertThat(firstToken.get("token_type"), is(DIRECT_DEBIT.toString()));
        assertThat(firstToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("issued_date"), is(inserted.format(DATE_TIME_FORMAT)));
    }

    @Test
    public void shouldReturnCardTokensIfTokenTypeIsNull() throws Exception {
        ZonedDateTime inserted = app.getDatabaseHelper().getCurrentTime();
        ZonedDateTime lastUsed = inserted.plusMinutes(30);
        app.getDatabaseHelper().insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, ACCOUNT_ID, null, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, lastUsed, null);

        List<Map<String, Object>> tokens = authTokenDao.findTokensWithStateById(ACCOUNT_ID, ACTIVE);

        assertThat(tokens.size(), is(1));

        Map<String, Object> firstToken = tokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.containsKey("revoked"), is(false));
        assertThat(firstToken.get("created_by"), is(TEST_USER_NAME_2));
        assertThat(firstToken.get("token_type"), is(CARD.toString()));
        assertThat(firstToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("issued_date"), is(inserted.format(DATE_TIME_FORMAT)));
    }

    @Test
    public void shouldFindRevokedCardTokens() throws Exception {
        ZonedDateTime inserted = app.getDatabaseHelper().getCurrentTime();
        ZonedDateTime lastUsed = inserted.plusMinutes(30);
        ZonedDateTime revoked = inserted.plusMinutes(45);
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, revoked, TEST_USER_NAME, lastUsed);
        app.getDatabaseHelper().insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, ACCOUNT_ID, null, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, lastUsed);

        List<Map<String, Object>> tokens = authTokenDao.findTokensWithStateById(ACCOUNT_ID, REVOKED);

        assertThat(tokens.size(), is(1));
        Map<String, Object> firstToken = tokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION));
        assertThat(firstToken.containsKey("revoked"), is(true));
        assertThat(firstToken.get("revoked"), is(revoked.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("created_by"), is(TEST_USER_NAME));
        assertThat(firstToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("issued_date"), is(inserted.format(DATE_TIME_FORMAT)));
    }

    @Test
    public void shouldFindRevokedTokensByExternalId() throws Exception {
        ZonedDateTime inserted = app.getDatabaseHelper().getCurrentTime();
        ZonedDateTime lastUsed = inserted.plusMinutes(30);
        ZonedDateTime revoked = inserted.plusMinutes(45);
        String accountExternalId = "2340dsadas";
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, accountExternalId, TOKEN_DESCRIPTION, revoked, TEST_USER_NAME, lastUsed, DIRECT_DEBIT);
        app.getDatabaseHelper().insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, ACCOUNT_ID, accountExternalId, TOKEN_DESCRIPTION_2, null, TEST_USER_NAME_2, lastUsed, DIRECT_DEBIT);


        List<Map<String, Object>> tokens = authTokenDao.findTokensWithStateByExternalId(accountExternalId, REVOKED);

        assertThat(tokens.size(), is(1));
        Map<String, Object> firstToken = tokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION));
        assertThat(firstToken.containsKey("revoked"), is(true));
        assertThat(firstToken.get("revoked"), is(revoked.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("created_by"), is(TEST_USER_NAME));
        assertThat(firstToken.get("last_used"), is(lastUsed.format(DATE_TIME_FORMAT)));
        assertThat(firstToken.get("issued_date"), is(inserted.format(DATE_TIME_FORMAT)));
    }

    @Test
    public void shouldInsertNewToken() {
        authTokenDao.storeToken("token-hash", "token-link", "account-id", "account-external-id", "description", "user", DIRECT_DEBIT);
        Map<String, Object> tokenByHash = app.getDatabaseHelper().getTokenByHash("token-hash");
        ZonedDateTime now = app.getDatabaseHelper().getCurrentTime();

        assertThat(tokenByHash.get("token_hash"), is("token-hash"));
        assertThat(tokenByHash.get("account_id"), is("account-id"));
        assertThat(tokenByHash.get("account_external_id"), is("account-external-id"));
        assertThat(tokenByHash.get("description"), is("description"));
        assertThat(tokenByHash.get("created_by"), is("user"));
        assertNull(tokenByHash.get("last_used"));
        assertThat(tokenByHash.get("token_type"), is(DIRECT_DEBIT.toString()));
        ZonedDateTime tokenIssueTime = app.getDatabaseHelper().issueTimestampForAccount("account-id");
        assertThat(tokenIssueTime, isCloseTo(now));
    }

    @Test
    public void shouldInsertNewTokenWithoutExternalId() {
        authTokenDao.storeToken("token-hash", "token-link", "account-id", null, "description", "user", CARD);
        Map<String, Object> tokenByHash = app.getDatabaseHelper().getTokenByHash("token-hash");
        ZonedDateTime now = app.getDatabaseHelper().getCurrentTime();

        assertThat(tokenByHash.get("token_hash"), is("token-hash"));
        assertThat(tokenByHash.get("account_id"), is("account-id"));
        assertThat(tokenByHash.get("account_external_id"), is(nullValue()));
        assertThat(tokenByHash.get("description"), is("description"));
        assertThat(tokenByHash.get("created_by"), is("user"));
        assertNull(tokenByHash.get("last_used"));
        assertThat(tokenByHash.get("token_type"), is(CARD.toString()));
        ZonedDateTime tokenIssueTime = app.getDatabaseHelper().issueTimestampForAccount("account-id");
        assertThat(tokenIssueTime, isCloseTo(now));
    }

    @Test
    public void updateAnExistingToken() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, TEST_USER_NAME);
        boolean updateResult = authTokenDao.updateTokenDescription(TOKEN_LINK, TOKEN_DESCRIPTION_2);

        assertThat(updateResult, is(true));
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION_2));
    }

    @Test
    public void shouldFindTokenByTokenLink() throws Exception {
        ZonedDateTime now = app.getDatabaseHelper().getCurrentTime();
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, null, TEST_USER_NAME, now, DIRECT_DEBIT);
        Optional<Map<String, Object>> tokenMayBe = authTokenDao.findTokenByTokenLink(TOKEN_LINK);
        Map<String, Object> token = tokenMayBe.get();

        assertThat(TOKEN_LINK, is(token.get("token_link")));
        assertThat(TOKEN_DESCRIPTION, is(token.get("description")));
        assertThat(TEST_USER_NAME, is(token.get("created_by")));
        assertThat(token.get("token_type"), is(DIRECT_DEBIT.toString()));
        assertThat(token.get("revoked"), is(nullValue()));
        assertThat(token.get("issued_date"), is(now.format(DATE_TIME_FORMAT)));
        assertThat(token.get("last_used"), is(now.format(DATE_TIME_FORMAT)));
    }

    @Test
    public void shouldFindByTokenLinkAndReturnCardTokensIfTokenTypeIsNull() throws Exception {
        ZonedDateTime now = app.getDatabaseHelper().getCurrentTime();
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, null, TEST_USER_NAME, now, null);
        Optional<Map<String, Object>> tokenMayBe = authTokenDao.findTokenByTokenLink(TOKEN_LINK);
        Map<String, Object> token = tokenMayBe.get();
        assertThat(TOKEN_LINK, is(token.get("token_link")));
        assertThat(TOKEN_DESCRIPTION, is(token.get("description")));
        assertThat(TEST_USER_NAME, is(token.get("created_by")));
        assertThat(token.get("token_type"), is(CARD.toString()));
        assertThat(token.get("revoked"), is(nullValue()));
        assertThat(token.get("issued_date"), is(now.format(DATE_TIME_FORMAT)));
        assertThat(token.get("last_used"), is(now.format(DATE_TIME_FORMAT)));
    }

    @Test
    public void notUpdateANonExistingToken() throws Exception {
        boolean updateResult = authTokenDao.updateTokenDescription(TOKEN_LINK, TOKEN_DESCRIPTION_2);

        assertThat(updateResult, is(false));
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.isPresent(), is(false));
    }

    @Test
    public void notUpdateARevokedToken() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, ZonedDateTime.now(), TEST_USER_NAME);

        boolean updateResult = authTokenDao.updateTokenDescription(TOKEN_LINK, TOKEN_DESCRIPTION_2);

        assertThat(updateResult, is(false));
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnForTokenTable("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
    }

    @Test
    public void shouldRevokeASingleToken() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, TEST_USER_NAME);

        Optional<String> revokedDate = authTokenDao.revokeSingleToken(ACCOUNT_ID, TOKEN_LINK);

        assertThat(revokedDate.get(), is(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("dd MMM YYYY"))));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK);
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    public void shouldNotRevokeATokenForAnotherAccount() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, TEST_USER_NAME);
        app.getDatabaseHelper().insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, ACCOUNT_ID_2, null, TOKEN_DESCRIPTION_2, TEST_USER_NAME);

        Optional<String> revokedDate  = authTokenDao.revokeSingleToken(ACCOUNT_ID, TOKEN_LINK_2);

        assertThat(revokedDate.isPresent(), is(false));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK);
        assertThat(revokedInDb.isPresent(), is(false));
    }

    @Test
    public void shouldNotRevokeATokenAlreadyRevoked() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, ZonedDateTime.now(), TEST_USER_NAME);

        Optional<String> revokedDate = authTokenDao.revokeSingleToken(ACCOUNT_ID, TOKEN_LINK);

        assertThat(revokedDate.isPresent(), is(false));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnForTokenTable("revoked", "token_link", TOKEN_LINK);
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test(expected = RuntimeException.class)
    public void shouldErrorIfTriesToSaveTheSameTokenTwice() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, TEST_USER_NAME);

        authTokenDao.storeToken(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, null, TOKEN_DESCRIPTION, "test@email.com", CARD);
    }

    private Matcher<ChronoZonedDateTime<?>> isCloseTo(ZonedDateTime now) {
        return both(greaterThan(now.minusSeconds(5))).and(lessThan(now.plusSeconds(5)));
    }
}
