package uk.gov.pay.publicauth.service;

import com.google.common.io.BaseEncoding;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.app.config.TokensConfiguration;
import uk.gov.pay.publicauth.auth.Token;
import uk.gov.pay.publicauth.dao.AuthTokenDao;
import uk.gov.pay.publicauth.exception.TokenNotFoundException;
import uk.gov.pay.publicauth.model.CreateTokenRequest;
import uk.gov.pay.publicauth.model.TokenHash;
import uk.gov.pay.publicauth.model.TokenLink;
import uk.gov.pay.publicauth.model.TokenResponse;
import uk.gov.pay.publicauth.model.TokenSource;
import uk.gov.pay.publicauth.model.TokenState;
import uk.gov.pay.publicauth.model.Tokens;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.pay.publicauth.service.RandomIdGenerator.RANDOM_ID_MAX_LENGTH;
import static uk.gov.pay.publicauth.service.RandomIdGenerator.RANDOM_ID_MIN_LENGTH;
import static uk.gov.pay.publicauth.service.RandomIdGenerator.newId;

public class TokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenService.class);

    private static final int HMAC_SHA1_LENGTH = 32;
    private static final int API_KEY_MIN_LENGTH = HMAC_SHA1_LENGTH + RANDOM_ID_MIN_LENGTH;
    private static final int API_KEY_MAX_LENGTH = HMAC_SHA1_LENGTH + RANDOM_ID_MAX_LENGTH;

    private final String encryptDBSalt;
    private final String apiKeyHmacSecret;
    private final AuthTokenDao authTokenDao;

    public TokenService(TokensConfiguration config, AuthTokenDao authTokenDao) {
        this.encryptDBSalt = config.getEncryptDBSalt();
        this.apiKeyHmacSecret = config.getApiKeyHmacSecret();
        this.authTokenDao = authTokenDao;
    }

    public String createTokenForAccount(CreateTokenRequest createTokenRequest) {
        Tokens tokens = issueTokens();
        authTokenDao.storeToken(tokens.getHashedToken(), createTokenRequest);
        LOGGER.info("Created token with token_link {}", createTokenRequest.getTokenLink());
        return tokens.getApiKey();
    }
    
    /**
     * Tokens includes:
     * - Salted BCrypt Hash. Intended to be used as encrypted value when storing in DB
     * - Token + Hmac(Token + SecretKey). To be used as API key
     */
    private Tokens issueTokens() {
        final String newId = newId();
        return new Tokens(encrypt(newId), createApiKey(newId));
    }

    /**
     * Tokens includes:
     * - ApiKey = Token + Hmac(Token + SecretKey).
     * - Extract Token, check Hmac and encrypt.
     */
    public Optional<Token> extractEncryptedTokenFrom(String apiKey) {
        if (isValidLength(apiKey)) {
            int initHmacIndex = apiKey.length() - HMAC_SHA1_LENGTH;
            String hmacFromApiKey = apiKey.substring(initHmacIndex);
            String tokenFromApiKey = apiKey.substring(0, initHmacIndex);
            if (tokenMatchesHmac(tokenFromApiKey, hmacFromApiKey)) {
                return Optional.of(new Token(encrypt(tokenFromApiKey)));
            }
            LOGGER.error("Authorisation failure - token does not match the given Hmac");
        }

        LOGGER.error("Authorisation failure - token extraction from key failed");
        return Optional.empty();
    }
    
    public List<TokenResponse> findTokensBy(String accountId, TokenState tokenState, TokenSource tokenSource) {
        return authTokenDao.findTokensBy(accountId, tokenState, tokenSource)
                .stream()
                .map(TokenResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public TokenResponse updateTokenDescription(TokenLink tokenLink, String description) {
        if (authTokenDao.updateTokenDescription(tokenLink, description)) {
            LOGGER.info("Updated description of token with token_link {}", tokenLink);
            return authTokenDao.findTokenByTokenLink(tokenLink).map(TokenResponse::fromEntity)
                    .orElseThrow(() -> new TokenNotFoundException("Could not update description of token with token_link " + tokenLink));
        }

        LOGGER.error("Could not update description of token with token_link " + tokenLink);
        throw new TokenNotFoundException("Could not update description of token with token_link " + tokenLink);
    }

    public ZonedDateTime revokeToken(String accountId, TokenHash tokenHash) {
        return authTokenDao.revokeSingleToken(accountId, tokenHash)
                .orElseThrow(() -> new TokenNotFoundException("Could not revoke token"));
    }
    
    public ZonedDateTime revokeToken(String accountId, TokenLink tokenLink) {
        return authTokenDao.revokeSingleToken(accountId, tokenLink)
                .orElseThrow(() -> new TokenNotFoundException("Could not revoke token with token_link " + tokenLink));
    }

    private TokenHash encrypt(String token) {
        return TokenHash.of(BCrypt.hashpw(token, encryptDBSalt));
    }

    private String createApiKey(String token) {
        byte[] hmacBytes = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, apiKeyHmacSecret).hmac(token);
        String encodedHmac = BaseEncoding.base32Hex().lowerCase().omitPadding().encode(hmacBytes);
        return token + encodedHmac;
    }

    private boolean tokenMatchesHmac(String token, String currentHmac) {
        final String hmacCalculatedFromToken = BaseEncoding.base32Hex()
                .lowerCase().omitPadding()
                .encode(new HmacUtils(HmacAlgorithms.HMAC_SHA_1, apiKeyHmacSecret).hmac(token));

        return hmacCalculatedFromToken.equals(currentHmac);
    }

    private boolean isValidLength(String apiKey) {
        int apiKeyLength = apiKey.length();
        return (apiKeyLength >= API_KEY_MIN_LENGTH)
                && (apiKeyLength <= API_KEY_MAX_LENGTH);
    }
}
