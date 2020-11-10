package uk.gov.pay.publicauth.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TokenAccountType {
    LIVE, TEST;

    @JsonCreator
    public static TokenAccountType fromString(String type) {
        return TokenAccountType.valueOf(type.toUpperCase());
    }
}
