package uk.gov.pay.publicauth.model;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TokenTypeTest {
    @Test
    public void shouldParse() {
        assertThat(TokenType.fromString("API"), is(TokenType.API));
    }
    @Test
    public void shouldReturnApiIfTypeIsMissing() {
        assertThat(TokenType.fromString(""), is(TokenType.API));
    }
    @Test
    public void shouldReturnApiIfTypeIsNull() {
        assertThat(TokenType.fromString(null), is(TokenType.API));
    }
}
