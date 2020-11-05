package uk.gov.pay.publicauth.model;

import java.util.Objects;

public class TokenLink {
    private final String tokenLink;

    private TokenLink(String tokenLink) {
        this.tokenLink = Objects.requireNonNull(tokenLink);
    }

    public static TokenLink of(String tokenLink) {
        return new TokenLink(tokenLink);
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other.getClass() == TokenLink.class) {
            TokenLink that = (TokenLink) other;
            return this.tokenLink.equals(that.tokenLink);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return tokenLink.hashCode();
    }

    @Override
    public String toString() {
        return tokenLink;
    }
}
