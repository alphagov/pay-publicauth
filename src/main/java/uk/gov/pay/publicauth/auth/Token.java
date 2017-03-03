package uk.gov.pay.publicauth.auth;

import java.security.Principal;

public class Token implements Principal {

    private final String name;

    public Token(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
