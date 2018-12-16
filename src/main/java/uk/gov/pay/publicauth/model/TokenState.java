package uk.gov.pay.publicauth.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum TokenState {
    REVOKED, ACTIVE;
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenState.class);

    public static TokenState fromString(final String state) {
        try {
            return TokenState.valueOf(state.toUpperCase());
        } catch (Exception e) {
            LOGGER.error("Unknown token state: " + state);
            return ACTIVE;
        }
    }
}
