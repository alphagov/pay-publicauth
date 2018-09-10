package uk.gov.pay.publicauth.model;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TokenSourceTest {
    @Test
    public void shouldParseApi() {
        assertThat(TokenSource.fromString("API"), is(TokenSource.API));
        assertThat(TokenSource.fromString("api"), is(TokenSource.API));
    }
    @Test
    public void shouldParseProducts() {
        assertThat(TokenSource.fromString("PRODUCTS"), is(TokenSource.PRODUCTS));
        assertThat(TokenSource.fromString("products"), is(TokenSource.PRODUCTS));
    }
    @Test
    public void shouldReturnApiIfTypeIsMissing() {
        assertThat(TokenSource.fromString(""), is(TokenSource.API));
    }
    @Test
    public void shouldReturnApiIfTypeIsNull() {
        assertThat(TokenSource.fromString(null), is(TokenSource.API));
    }
}
