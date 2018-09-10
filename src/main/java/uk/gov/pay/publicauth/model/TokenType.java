package uk.gov.pay.publicauth.model;

public enum TokenType {
    API, PRODUCTS;

    public static TokenType fromString(final String type) {
        for (TokenType typeEnum : values()) {
            if (typeEnum.toString().equalsIgnoreCase(type)) {
                return typeEnum;
            }
        }
        return API;
    }
}
