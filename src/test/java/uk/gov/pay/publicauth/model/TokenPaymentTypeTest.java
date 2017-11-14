package uk.gov.pay.publicauth.model;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class TokenPaymentTypeTest {

    @Test
    public void shouldParseDirectDebit() {
        assertThat(TokenPaymentType.fromString("DIRECT_DEBIT"), is(TokenPaymentType.DIRECT_DEBIT));
    }
    @Test
    public void shouldReturnActiveTokensIfTypeIsMissing() {
        assertThat(TokenPaymentType.fromString(""), is(TokenPaymentType.CARD));
    }
}
