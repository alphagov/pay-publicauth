package uk.gov.pay.publicauth.auth;

import io.dropwizard.auth.Authenticator;
import uk.gov.pay.publicauth.service.TokenService;

import java.util.Optional;

public class TokenAuthenticator implements Authenticator<String, Token> {

    private TokenService tokenService;

    public TokenAuthenticator(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public Optional<Token> authenticate(String bearerToken) {
        return tokenService.extractEncryptedTokenFrom(bearerToken);
    }
}
