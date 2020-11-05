package uk.gov.pay.publicauth.exception;

import uk.gov.pay.publicauth.model.TokenLink;

import static java.lang.String.format;

public class TokenRevokedException extends RuntimeException {
    private TokenLink tokenLink;

    public TokenRevokedException(TokenLink tokenLink) {
        super(format("Token with token_link %s has been revoked", tokenLink));
        this.tokenLink = tokenLink;
    }

    public TokenLink getTokenLink() {
        return tokenLink;
    }
}
