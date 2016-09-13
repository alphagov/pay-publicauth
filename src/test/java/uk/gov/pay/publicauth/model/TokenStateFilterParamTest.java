package uk.gov.pay.publicauth.model;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class TokenStateFilterParamTest {

    @Test
    public void shouldParseActiveTokens() {
        assertThat(TokenStateFilterParam.fromString("active"), is(TokenStateFilterParam.ACTIVE));
        assertThat(TokenStateFilterParam.fromString("ACTIVE"), is(TokenStateFilterParam.ACTIVE));
    }

    @Test
    public void shouldParseRevokedTokens() {
        assertThat(TokenStateFilterParam.fromString("revoked"), is(TokenStateFilterParam.REVOKED));
        assertThat(TokenStateFilterParam.fromString("REVOKED"), is(TokenStateFilterParam.REVOKED));
    }
    @Test
    public void shouldReturnActiveTokensIfStateIsUnknown() {
        assertThat(TokenStateFilterParam.fromString("somethingelse"), is(TokenStateFilterParam.ACTIVE));
    }
}
