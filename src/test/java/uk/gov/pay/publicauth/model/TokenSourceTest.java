package uk.gov.pay.publicauth.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TokenSourceTest {
    @Test
    void shouldParseApi() {
        assertThat(TokenSource.fromString("API"), is(TokenSource.API));
        assertThat(TokenSource.fromString("api"), is(TokenSource.API));
    }
    @Test
    void shouldParseProducts() {
        assertThat(TokenSource.fromString("PRODUCTS"), is(TokenSource.PRODUCTS));
        assertThat(TokenSource.fromString("products"), is(TokenSource.PRODUCTS));
    }
    @Test
    void shouldReturnApiIfTypeIsMissing() {
        assertThat(TokenSource.fromString(""), is(TokenSource.API));
    }
    @Test
    void shouldReturnApiIfTypeIsNull() {
        assertThat(TokenSource.fromString(null), is(TokenSource.API));
    }
}
