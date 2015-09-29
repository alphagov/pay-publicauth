package uk.gov.pay.publicauth.dao;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.gov.pay.publicauth.utils.DropwizardAppWithPostgresRule;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AuthTokenDaoTest {

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private AuthTokenDao chargeDao;
    private static final String ACCOUNT_ID = "564532435";
    private static final String BEARER_TOKEN = "TOKEN";

    @Before
    public void setUp() throws Exception {
        chargeDao = new AuthTokenDao(app.getJdbi());
    }

    @Test
    public void findAnAccountIdByToken() throws Exception {
        app.getDatabaseHelper().insertAccount(BEARER_TOKEN, ACCOUNT_ID);

        Optional<String> chargeId = chargeDao.findAccount(BEARER_TOKEN);
        assertThat(chargeId, is(Optional.of(ACCOUNT_ID)));
    }

    @Test
    public void missingTokenHasNoAccount() throws Exception {
        Optional<String> chargeId = chargeDao.findAccount(BEARER_TOKEN);
        assertThat(chargeId, is(Optional.empty()));
    }


}