package uk.gov.pay.publicauth.model;


import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


class TokenStateTest {

    @Test
    void shouldParseActiveTokens() {
        MatcherAssert.assertThat(TokenState.fromString("active"), is(TokenState.ACTIVE));
        MatcherAssert.assertThat(TokenState.fromString("ACTIVE"), is(TokenState.ACTIVE));
    }

    @Test
    void shouldParseRevokedTokens() {
        MatcherAssert.assertThat(TokenState.fromString("revoked"), is(TokenState.REVOKED));
        MatcherAssert.assertThat(TokenState.fromString("REVOKED"), is(TokenState.REVOKED));
    }
    @Test
    void shouldReturnActiveTokensIfStateIsUnknown() {
        MatcherAssert.assertThat(TokenState.fromString("somethingelse"), is(TokenState.ACTIVE));
    }
}
