package uk.gov.pay.publicauth.service;

import com.google.common.base.Optional;
import com.google.common.io.BaseEncoding;
import org.apache.commons.codec.digest.HmacUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.model.Tokens;
import uk.gov.pay.publicauth.util.RandomIdGenerator;


public class TokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenService.class);

    private static final String HASH_SALT = "$2a$10$IhaXo6LIBhKIWOiGpbtPOu";
    private static final String HMAC_SECRET_KEY = "qwer9yuhgf";
    private static final int HMAC_SHA1_LENGTH = 32;

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
        return BCrypt.hashpw(token, HASH_SALT);
    }

    private String createApiKey(String token) {
        byte[] hmacBytes = HmacUtils.hmacSha1(HMAC_SECRET_KEY, token);
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
                .encode(HmacUtils.hmacSha1(HMAC_SECRET_KEY, token));

        return hmacCalculatedFromToken.equals(currentHmac);
    }
}
