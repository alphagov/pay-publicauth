package uk.gov.pay.publicauth.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ServiceMode {
    LIVE, TEST;

    @JsonCreator
    public static ServiceMode fromString(String type) {
        return ServiceMode.valueOf(type.toUpperCase());
    }
}
