package uk.gov.pay.publicauth.model;


import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;


class TokenPaymentTypeTest {

    @Test
    void shouldParseDirectDebit() {
        MatcherAssert.assertThat(TokenPaymentType.fromString("DIRECT_DEBIT"), is(TokenPaymentType.DIRECT_DEBIT));
        MatcherAssert.assertThat(TokenPaymentType.fromString("direct_debit"), is(TokenPaymentType.DIRECT_DEBIT));
    }
    @Test
    void shouldParseCard() {
        MatcherAssert.assertThat(TokenPaymentType.fromString("CARD"), is(TokenPaymentType.CARD));
        MatcherAssert.assertThat(TokenPaymentType.fromString("card"), is(TokenPaymentType.CARD));
    }
    @Test
    void shouldReturnCardIfTypeIsMissing() {
        MatcherAssert.assertThat(TokenPaymentType.fromString(""), is(TokenPaymentType.CARD));
    }
    @Test
    void shouldReturnCardIfTypeIsNull() {
        MatcherAssert.assertThat(TokenPaymentType.fromString(null), is(TokenPaymentType.CARD));
    }
}
