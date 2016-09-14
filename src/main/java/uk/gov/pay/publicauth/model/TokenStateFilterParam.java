package uk.gov.pay.publicauth.model;

public enum TokenStateFilterParam {
    REVOKED, ACTIVE;

    public static TokenStateFilterParam fromString(final String state) {
        try {
            return TokenStateFilterParam.valueOf(state.toUpperCase());
        } catch (Exception e) {
            return ACTIVE;
        }
    }
}