package uk.gov.pay.publicauth.model;


import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TokenPaymentTypeTest {

    @Test
    void shouldParseDirectDebit() {
        assertThat(TokenPaymentType.fromString("DIRECT_DEBIT"), is(TokenPaymentType.DIRECT_DEBIT));
        assertThat(TokenPaymentType.fromString("direct_debit"), is(TokenPaymentType.DIRECT_DEBIT));
    }
    @Test
    void shouldParseCard() {
        assertThat(TokenPaymentType.fromString("CARD"), is(TokenPaymentType.CARD));
        assertThat(TokenPaymentType.fromString("card"), is(TokenPaymentType.CARD));
    }
    @Test
    void shouldReturnCardIfTypeIsMissing() {
        assertThat(TokenPaymentType.fromString(""), is(TokenPaymentType.CARD));
    }
    @Test
    void shouldReturnCardIfTypeIsNull() {
        assertThat(TokenPaymentType.fromString(null), is(TokenPaymentType.CARD));
    }
}
