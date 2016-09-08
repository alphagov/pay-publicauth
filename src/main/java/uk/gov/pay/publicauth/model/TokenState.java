package uk.gov.pay.publicauth.model;

public enum TokenState {
    ALL, ACTIVE;

    public static TokenState fromString(final String state) {
        try {
            return TokenState.valueOf(state.toUpperCase());
        } catch (Exception e) {
            return ACTIVE;
        }
    }
}