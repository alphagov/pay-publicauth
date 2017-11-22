package uk.gov.pay.publicauth.model;

public enum TokenPaymentType {
    CARD, DIRECT_DEBIT;

    public static TokenPaymentType fromString(final String type) {
        try {
            return TokenPaymentType.valueOf(type);
        } catch (Exception e) {
            return CARD;
        }
    }
}
