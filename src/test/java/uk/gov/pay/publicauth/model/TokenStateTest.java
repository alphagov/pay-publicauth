package uk.gov.pay.publicauth.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TokenStateTest {

    @Test
    void shouldParseActiveTokens() {
        assertThat(TokenState.fromString("active"), is(TokenState.ACTIVE));
        assertThat(TokenState.fromString("ACTIVE"), is(TokenState.ACTIVE));
    }

    @Test
    void shouldParseRevokedTokens() {
        assertThat(TokenState.fromString("revoked"), is(TokenState.REVOKED));
        assertThat(TokenState.fromString("REVOKED"), is(TokenState.REVOKED));
    }
    @Test
    void shouldReturnActiveTokensIfStateIsUnknown() {
        assertThat(TokenState.fromString("somethingelse"), is(TokenState.ACTIVE));
    }
}
