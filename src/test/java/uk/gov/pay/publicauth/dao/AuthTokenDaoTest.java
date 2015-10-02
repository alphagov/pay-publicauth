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
import static org.joda.time.DateTimeZone.UTC;

public class AuthTokenDaoTest {

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private AuthTokenDao authTokenDao;
    private static final String ACCOUNT_ID = "564532435";
    private static final String BEARER_TOKEN = "TOKEN";

    @Before
    public void setUp() throws Exception {
        authTokenDao = new AuthTokenDao(app.getJdbi());
    }

    @Test
    public void findAnAccountIdByToken() throws Exception {
        app.getDatabaseHelper().insertAccount(BEARER_TOKEN, ACCOUNT_ID);

        Optional<String> chargeId = authTokenDao.findAccount(BEARER_TOKEN);
        assertThat(chargeId, is(Optional.of(ACCOUNT_ID)));
    }

    @Test
    public void missingTokenHasNoAccount() throws Exception {
        Optional<String> accountId = authTokenDao.findAccount(BEARER_TOKEN);
        assertThat(accountId, is(Optional.empty()));
    }

    @Test
    public void shouldInsertANewToken() throws Exception {
        String expectedAccountId = "an-account";
        authTokenDao.storeToken(BEARER_TOKEN, expectedAccountId);
        Optional<String> storedAccountId = authTokenDao.findAccount(BEARER_TOKEN);
        assertThat(storedAccountId, is(Optional.of(expectedAccountId)));

        DateTime issueTimestamp = app.getDatabaseHelper().issueTimestampForAccount(expectedAccountId);
        DateTime now = DateTime.now(UTC);

        assertThat(issueTimestamp, isCloseTo(now));
    }

    @Test
    public void shouldAllowTokensToBeRevoked() throws Exception {
        app.getDatabaseHelper().insertAccount(BEARER_TOKEN, ACCOUNT_ID);
        assertThat(authTokenDao.revokeToken(ACCOUNT_ID), is(true));
        assertThat(authTokenDao.findAccount(ACCOUNT_ID), is(Optional.empty()));

        DateTime revokeTimestamp = app.getDatabaseHelper().revokeTimestampForAccount(ACCOUNT_ID);
        DateTime now = DateTime.now(UTC);

        assertThat(revokeTimestamp, isCloseTo(now));
    }

    @Test(expected = RuntimeException.class)
    public void shouldErrorIfTriesToSaveTheSameTokenTwice() throws Exception {
        app.getDatabaseHelper().insertAccount(BEARER_TOKEN, ACCOUNT_ID);

        authTokenDao.storeToken(BEARER_TOKEN, ACCOUNT_ID);
    }

    private Matcher<ReadableInstant> isCloseTo(DateTime now) {
        return both(greaterThan(now.minusSeconds(5))).and(lessThan(now.plusSeconds(5)));
    }
}