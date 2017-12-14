package uk.gov.pay.publicauth.service;

import com.google.common.io.BaseEncoding;
import org.apache.commons.codec.digest.HmacUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.app.config.TokensConfiguration;
import uk.gov.pay.publicauth.auth.Token;
import uk.gov.pay.publicauth.model.Tokens;

import java.util.Optional;

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

    public TokenService(TokensConfiguration config) {
        this.encryptDBSalt = config.getEncryptDBSalt();
        this.apiKeyHmacSecret = config.getApiKeyHmacSecret();
    }

    /**
     * Tokens includes:
     * - Salted BCrypt Hash. Intended to be used as encrypted value when storing in DB
     * - Token + Hmac(Token + SecretKey). To be used as API key
     */
    public Tokens issueTokens() {
        final String newId = newId();
        return new Tokens(encrypt(newId), createApiKey(newId));
    }

    /**
     * Tokens includes:
     * - ApiKey = Token + Hmac(Token + SecretKey).
     * - Extract Token, check Hmac and encrypt.
     */
    public Optional<Token> extractEncryptedTokenFrom(String apiKey) {
        return extractAndEncryptTokenFrom(apiKey);
    }

    private String encrypt(String token) {
        return BCrypt.hashpw(token, encryptDBSalt);
    }

    private String createApiKey(String token) {
        byte[] hmacBytes = HmacUtils.hmacSha1(apiKeyHmacSecret, token);
        String encodedHmac = BaseEncoding.base32Hex().lowerCase().omitPadding().encode(hmacBytes);
        return token + encodedHmac;
    }

    private Optional<Token> extractAndEncryptTokenFrom(String apiKey) {
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

    private boolean tokenMatchesHmac(String token, String currentHmac) {
        final String hmacCalculatedFromToken = BaseEncoding.base32Hex()
                .lowerCase().omitPadding()
                .encode(HmacUtils.hmacSha1(apiKeyHmacSecret, token));

        return hmacCalculatedFromToken.equals(currentHmac);
    }

    private boolean isValidLength(String apiKey) {
        int apiKeyLength = apiKey.length();
        return (apiKeyLength >= API_KEY_MIN_LENGTH)
                && (apiKeyLength <= API_KEY_MAX_LENGTH);
    }
}
