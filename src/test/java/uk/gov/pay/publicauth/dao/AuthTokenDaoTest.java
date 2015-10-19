package uk.gov.pay.publicauth.dao;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresRule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;

public class AuthTokenDaoTest {

    private  DateTime now = DateTime.now();

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
    public void findAnAccountIdByToken() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        Optional<String> accountId = authTokenDao.findAccount(TOKEN_HASH);
        assertThat(accountId, is(Optional.of(ACCOUNT_ID)));
    }

    @Test
    public void missingTokenHasNoAccount() throws Exception {
        Optional<String> accountId = authTokenDao.findAccount(TOKEN_HASH);
        assertThat(accountId, is(Optional.empty()));
    }

    @Test
    public void missingAccountHasNoAssociatedTokens() throws Exception {
        List<Map<String, Object>> tokens = authTokenDao.findTokens(ACCOUNT_ID);
        assertThat(tokens, is(Lists.newArrayList()));
    }

    @Test
    public void accountWithSeveralTokens() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, true);
        app.getDatabaseHelper().insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION_2);

        List<Map<String, Object>> tokens = authTokenDao.findTokens(ACCOUNT_ID);

        //Retrieved in issued order from newest to oldest
        Map<String, Object> firstToken = tokens.get(0);
        assertThat(firstToken.get("token_link"), is(TOKEN_LINK_2));
        assertThat(firstToken.get("description"), is(TOKEN_DESCRIPTION_2));
        assertThat(firstToken.get("revoked"), nullValue());

        Map<String, Object> secondToken = tokens.get(1);
        assertThat(secondToken.get("token_link"), is(TOKEN_LINK));
        assertThat(secondToken.get("description"), is(TOKEN_DESCRIPTION));
        assertThat(secondToken.get("revoked"), is(now.toString("dd MMM YYYY")));
    }

    @Test
    public void shouldInsertANewToken() throws Exception {
        String expectedAccountId = "an-account";
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, expectedAccountId, TOKEN_DESCRIPTION);

        Optional<String> storedAccountId = authTokenDao.findAccount(TOKEN_HASH);
        assertThat(storedAccountId, is(Optional.of(expectedAccountId)));

        DateTime issueTimestamp = app.getDatabaseHelper().issueTimestampForAccount(expectedAccountId);
        DateTime now = app.getDatabaseHelper().getCurrentTime();

        assertThat(issueTimestamp, isCloseTo(now));
    }

    @Test
    public void updateAnExistingToken() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        boolean updateResult = authTokenDao.updateTokenDescription(TOKEN_LINK, TOKEN_DESCRIPTION_2);

        assertThat(updateResult, is(true));
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnFor("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION_2));
    }

    @Test
    public void notUpdateANonExistingToken() throws Exception {
        boolean updateResult = authTokenDao.updateTokenDescription(TOKEN_LINK, TOKEN_DESCRIPTION_2);

        assertThat(updateResult, is(false));
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnFor("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.isPresent(), is(false));
    }

    @Test
    public void notUpdateARevokedToken() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, true);

        boolean updateResult = authTokenDao.updateTokenDescription(TOKEN_LINK, TOKEN_DESCRIPTION_2);

        assertThat(updateResult, is(false));
        Optional<String> descriptionInDb = app.getDatabaseHelper().lookupColumnFor("description", "token_link", TOKEN_LINK);
        assertThat(descriptionInDb.get(), equalTo(TOKEN_DESCRIPTION));
    }

    @Test
    public void shouldRevokeASingleToken() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        Optional<String> revokedDate = authTokenDao.revokeSingleToken(ACCOUNT_ID, TOKEN_LINK);

        assertThat(revokedDate.get(), is(now.toString("dd MMM YYYY")));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnFor("revoked", "token_link", TOKEN_LINK);
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    public void shouldNotRevokeATokenForAnotherAccount() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);
        app.getDatabaseHelper().insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, ACCOUNT_ID_2, TOKEN_DESCRIPTION_2);

        Optional<String> revokedDate  = authTokenDao.revokeSingleToken(ACCOUNT_ID, TOKEN_LINK_2);

        assertThat(revokedDate.isPresent(), is(false));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnFor("revoked", "token_link", TOKEN_LINK);
        assertThat(revokedInDb.isPresent(), is(false));
    }

    @Test
    public void shouldNotRevokeATokenAlreadyRevoked() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION, true);

        Optional<String> revokedDate = authTokenDao.revokeSingleToken(ACCOUNT_ID, TOKEN_LINK);

        assertThat(revokedDate.isPresent(), is(false));

        Optional<String> revokedInDb = app.getDatabaseHelper().lookupColumnFor("revoked", "token_link", TOKEN_LINK);
        assertThat(revokedInDb.isPresent(), is(true));
    }

    @Test
    public void shouldAllowMultipleTokensToBeRevoked_forOneIssuedToken() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        assertThat(authTokenDao.revokeMultipleTokens(ACCOUNT_ID), is(true));
        assertThat(authTokenDao.findAccount(TOKEN_HASH), is(Optional.empty()));

        DateTime revokeTimestamp = app.getDatabaseHelper().revokeTimestampForAccount(ACCOUNT_ID);
        DateTime now = app.getDatabaseHelper().getCurrentTime();

        assertThat(revokeTimestamp, isCloseTo(now));
    }

    @Test
    public void shouldAllowMultipleTokensToBeRevoked_forTwoIssuedTokens() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);
        app.getDatabaseHelper().insertAccount(TOKEN_HASH_2, TOKEN_LINK_2, ACCOUNT_ID, TOKEN_DESCRIPTION);

        assertThat(authTokenDao.revokeMultipleTokens(ACCOUNT_ID), is(true));
        assertThat(authTokenDao.findAccount(TOKEN_HASH), is(Optional.empty()));
        assertThat(authTokenDao.findAccount(TOKEN_HASH_2), is(Optional.empty()));

        DateTime revokeTimestamp = app.getDatabaseHelper().revokeTimestampForAccount(ACCOUNT_ID);
        DateTime now = app.getDatabaseHelper().getCurrentTime();

        assertThat(revokeTimestamp, isCloseTo(now));
    }


    @Test(expected = RuntimeException.class)
    public void shouldErrorIfTriesToSaveTheSameTokenTwice() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);

        authTokenDao.storeToken(TOKEN_HASH, TOKEN_LINK, ACCOUNT_ID, TOKEN_DESCRIPTION);
    }

    private Matcher<ReadableInstant> isCloseTo(DateTime now) {
        return both(greaterThan(now.minusSeconds(5))).and(lessThan(now.plusSeconds(5)));
    }
}