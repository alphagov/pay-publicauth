package uk.gov.pay.publicauth.auth;

import uk.gov.pay.publicauth.model.TokenPaymentType;

import java.security.Principal;

public class Token implements Principal {

    private final String name;
    private final TokenPaymentType tokenPaymentType;

    public Token(String name) {
        this(name, TokenPaymentType.CARD);
}

    public Token(String name, TokenPaymentType tokenPaymentType) {
        this.name = name;
        this.tokenPaymentType = tokenPaymentType;
    }

    @Override
    public String getName() {
        return name;
    }

    public TokenPaymentType getTokenPaymentType() {
        return tokenPaymentType;
    }
}
