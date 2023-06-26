package uk.gov.pay.publicauth.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum TokenPaymentType {
    CARD, DIRECT_DEBIT;
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenPaymentType.class);

    public static TokenPaymentType fromString(final String type) {
        try {
            return TokenPaymentType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            LOGGER.error("Unknown token payment type: {}", type);
            return CARD;
        }
    }
}
