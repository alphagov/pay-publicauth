package uk.gov.pay.publicauth.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum TokenSource {
    API, PRODUCTS, DEMO;
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenSource.class);

    public static TokenSource fromString(final String source) {
        try {
            return TokenSource.valueOf(source.toUpperCase());
        } catch (Exception e) {
            LOGGER.error("Unknown token source: " + source);
            return API;
        }
    }
}
