package uk.gov.pay.publicauth.auth;

import uk.gov.pay.publicauth.model.TokenHash;

import java.security.Principal;

public class Token implements Principal {

    private final TokenHash name;

    public Token(TokenHash name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name.getValue();
    }
}
