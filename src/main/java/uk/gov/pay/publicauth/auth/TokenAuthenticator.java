package uk.gov.pay.publicauth.auth;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import uk.gov.pay.publicauth.service.TokenService;

public class TokenAuthenticator implements Authenticator<String, String> {

    private TokenService tokenService;

    public TokenAuthenticator(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public Optional<String> authenticate(String bearerToken) throws AuthenticationException {
        return tokenService.extractEncryptedTokenFrom(bearerToken);
    }
}
