package uk.gov.pay.publicauth.model;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class TokenPaymentTypeTest {

    @Test
    public void shouldParseDirectDebit() {
        assertThat(TokenPaymentType.fromString("DIRECT_DEBIT"), is(TokenPaymentType.DIRECT_DEBIT));
        assertThat(TokenPaymentType.fromString("direct_debit"), is(TokenPaymentType.DIRECT_DEBIT));
    }
    @Test
    public void shouldParseCard() {
        assertThat(TokenPaymentType.fromString("CARD"), is(TokenPaymentType.CARD));
        assertThat(TokenPaymentType.fromString("card"), is(TokenPaymentType.CARD));
    }
    @Test
    public void shouldReturnCardIfTypeIsMissing() {
        assertThat(TokenPaymentType.fromString(""), is(TokenPaymentType.CARD));
    }
    @Test
    public void shouldReturnCardIfTypeIsNull() {
        assertThat(TokenPaymentType.fromString(null), is(TokenPaymentType.CARD));
    }
}
