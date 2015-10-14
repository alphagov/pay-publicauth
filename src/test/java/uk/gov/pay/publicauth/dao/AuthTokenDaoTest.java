package uk.gov.pay.publicauth.dao;

import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresRule;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AuthTokenDaoTest {

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private AuthTokenDao authTokenDao;
    private static final String ACCOUNT_ID = "564532435";
    private static final String TOKEN_HASH = "TOKEN";
    private static final String TOKEN_DESCRIPTION = "Token description";

    @Before
    public void setUp() throws Exception {
        authTokenDao = new AuthTokenDao(app.getJdbi());
    }

    @Test
    public void findAnAccountIdByToken() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, ACCOUNT_ID, TOKEN_DESCRIPTION);

        Optional<String> chargeId = authTokenDao.findAccount(TOKEN_HASH);
        assertThat(chargeId, is(Optional.of(ACCOUNT_ID)));
    }

    @Test
    public void findDescriptionByToken() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, ACCOUNT_ID, TOKEN_DESCRIPTION);

        Optional<String> description = authTokenDao.findDescription(TOKEN_HASH);
        assertThat(description, is(Optional.of(TOKEN_DESCRIPTION)));
    }

    @Test
    public void missingTokenHasNoAccountNorDescription() throws Exception {
        Optional<String> accountId = authTokenDao.findAccount(TOKEN_HASH);
        assertThat(accountId, is(Optional.empty()));

        Optional<String> description = authTokenDao.findDescription(TOKEN_HASH);
        assertThat(description, is(Optional.empty()));
    }

    @Test
    public void shouldInsertANewToken() throws Exception {
        String expectedAccountId = "an-account";
        authTokenDao.storeToken(TOKEN_HASH, expectedAccountId, TOKEN_DESCRIPTION);
        Optional<String> storedAccountId = authTokenDao.findAccount(TOKEN_HASH);
        assertThat(storedAccountId, is(Optional.of(expectedAccountId)));

        DateTime issueTimestamp = app.getDatabaseHelper().issueTimestampForAccount(expectedAccountId);
        DateTime now = app.getDatabaseHelper().getCurrentTime();

        assertThat(issueTimestamp, isCloseTo(now));
    }

    @Test
    public void shouldAllowATokenToBeRevoked() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, ACCOUNT_ID, TOKEN_DESCRIPTION);
        assertThat(authTokenDao.revokeToken(ACCOUNT_ID), is(true));
        assertThat(authTokenDao.findAccount(ACCOUNT_ID), is(Optional.empty()));

        DateTime revokeTimestamp = app.getDatabaseHelper().revokeTimestampForAccount(ACCOUNT_ID);
        DateTime now = app.getDatabaseHelper().getCurrentTime();

        assertThat(revokeTimestamp, isCloseTo(now));
    }

    @Test
    public void shouldAllowTokensToBeRevoked() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, ACCOUNT_ID, TOKEN_DESCRIPTION);
        app.getDatabaseHelper().insertAccount("TOKEN_2", ACCOUNT_ID, TOKEN_DESCRIPTION);
        assertThat(authTokenDao.revokeToken(ACCOUNT_ID), is(true));
        assertThat(authTokenDao.findAccount(ACCOUNT_ID), is(Optional.empty()));

        DateTime revokeTimestamp = app.getDatabaseHelper().revokeTimestampForAccount(ACCOUNT_ID);
        DateTime now = app.getDatabaseHelper().getCurrentTime();

        assertThat(revokeTimestamp, isCloseTo(now));
    }


    @Test(expected = RuntimeException.class)
    public void shouldErrorIfTriesToSaveTheSameTokenTwice() throws Exception {
        app.getDatabaseHelper().insertAccount(TOKEN_HASH, ACCOUNT_ID, TOKEN_DESCRIPTION);

        authTokenDao.storeToken(TOKEN_HASH, ACCOUNT_ID, TOKEN_DESCRIPTION);
    }

    private Matcher<ReadableInstant> isCloseTo(DateTime now) {
        return both(greaterThan(now.minusSeconds(5))).and(lessThan(now.plusSeconds(5)));
    }
}