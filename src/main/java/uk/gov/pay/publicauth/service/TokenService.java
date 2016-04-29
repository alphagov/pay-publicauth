package uk.gov.pay.publicauth.service;

import com.google.common.base.Optional;
import com.google.common.io.BaseEncoding;
import org.apache.commons.codec.digest.HmacUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.app.config.TokensConfiguration;
import uk.gov.pay.publicauth.model.Tokens;
import uk.gov.pay.publicauth.util.RandomIdGenerator;


public class TokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenService.class);

    private static final int HMAC_SHA1_LENGTH = 32;

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
        final String newId = RandomIdGenerator.newId();
        return new Tokens(encrypt(newId), createApiKey(newId));
    }

    /**
     * Tokens includes:
     * - ApiKey = Token + Hmac(Token + SecretKey).
     * - Extract Token, check Hmac and encrypt.
     */
    public Optional<String> extractEncryptedTokenFrom(String apiKey) {
        String token = extractTokenFrom(apiKey);
        if (token != null) {
            return Optional.of(encrypt(token));
        }
        return Optional.absent();
    }

    private String encrypt(String token) {
        return BCrypt.hashpw(token, encryptDBSalt);
    }

    private String createApiKey(String token) {
        byte[] hmacBytes = HmacUtils.hmacSha1(apiKeyHmacSecret, token);
        String encodedHmac = BaseEncoding.base32Hex().lowerCase().omitPadding().encode(hmacBytes);
        return token + encodedHmac;
    }

    private String extractTokenFrom(String apiKey) {
        String token = null;
        if (apiKey.length() >= HMAC_SHA1_LENGTH + 1) {
            int initHmacIndex = apiKey.length() - HMAC_SHA1_LENGTH;
            String hmacFromApiKey = apiKey.substring(initHmacIndex);
            String tokenFromApiKey = apiKey.substring(0, initHmacIndex);
            if (tokenMatchesHmac(tokenFromApiKey, hmacFromApiKey)) {
                token = tokenFromApiKey;
                LOGGER.error("Authorization token does not match the given Hmac");
            }
        }
        return token;
    }

    private boolean tokenMatchesHmac(String token, String currentHmac) {
        final String hmacCalculatedFromToken = BaseEncoding.base32Hex()
                .lowerCase().omitPadding()
                .encode(HmacUtils.hmacSha1(apiKeyHmacSecret, token));

        return hmacCalculatedFromToken.equals(currentHmac);
    }
}
