package uk.gov.pay.publicauth.model;


import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

class TokenSourceTest {
    @Test
    void shouldParseApi() {
        MatcherAssert.assertThat(TokenSource.fromString("API"), is(TokenSource.API));
        MatcherAssert.assertThat(TokenSource.fromString("api"), is(TokenSource.API));
    }
    @Test
    void shouldParseProducts() {
        MatcherAssert.assertThat(TokenSource.fromString("PRODUCTS"), is(TokenSource.PRODUCTS));
        MatcherAssert.assertThat(TokenSource.fromString("products"), is(TokenSource.PRODUCTS));
    }
    @Test
    void shouldReturnApiIfTypeIsMissing() {
        MatcherAssert.assertThat(TokenSource.fromString(""), is(TokenSource.API));
    }
    @Test
    void shouldReturnApiIfTypeIsNull() {
        MatcherAssert.assertThat(TokenSource.fromString(null), is(TokenSource.API));
    }
}
