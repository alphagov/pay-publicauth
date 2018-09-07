package uk.gov.pay.publicauth.model;

import java.util.Objects;

public class TokenHash implements TokenIdentifier {
    private final String tokenHash;

    private TokenHash (String tokenHash) {
        this.tokenHash = Objects.requireNonNull(tokenHash);
    }

    public static TokenHash of(String tokenHash) {
        return new TokenHash (tokenHash);
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other.getClass() == TokenHash .class) {
            TokenHash that = (TokenHash) other;
            return this.tokenHash.equals(that.tokenHash);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return tokenHash.hashCode();
    }

    //make sure we never serialise the token value
    @Override
    public String toString() {
        return "token_hash";
    }

    @Override
    public String getColumnName() {
        return "token_hash";
    }

    @Override
    public String getValue() {
        return tokenHash;
    }
}
