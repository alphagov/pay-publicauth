package uk.gov.pay.publicauth.model;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class TokenStateTest {

    @Test
    public void shouldParseActiveTokens() {
        assertThat(TokenState.fromString("active"), is(TokenState.ACTIVE));
        assertThat(TokenState.fromString("ACTIVE"), is(TokenState.ACTIVE));
    }

    @Test
    public void shouldParseRevokedTokens() {
        assertThat(TokenState.fromString("revoked"), is(TokenState.REVOKED));
        assertThat(TokenState.fromString("REVOKED"), is(TokenState.REVOKED));
    }
    @Test
    public void shouldReturnActiveByIfStateIsUnknown() {
        assertThat(TokenState.fromString("somethingelse"), is(TokenState.ACTIVE));
    }
}