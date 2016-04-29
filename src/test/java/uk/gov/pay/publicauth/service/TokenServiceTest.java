package uk.gov.pay.publicauth.service;

import com.google.common.base.Optional;
import com.google.common.io.BaseEncoding;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.Before;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;
import uk.gov.pay.publicauth.model.Tokens;

import java.util.List;

import static com.google.common.primitives.Chars.asList;
import static org.apache.commons.collections4.CollectionUtils.containsAll;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class TokenServiceTest {

    private static final String EXPECTED_SALT = "$2a$10$IhaXo6LIBhKIWOiGpbtPOu";
    private static final String EXPECTED_SECRET_KEY = "qwer9yuhgf";
    private static final List<Character> BASE32_HEX_DICTIONARY = asList("0123456789abcdefghijklmnopqrstuv".toCharArray());

    private TokenService tokenService;

    @Before
    public void setup() {
        tokenService = new TokenService();
    }

    @Test
    public void issueTokens_shouldIssueValidHashedToken() {

        Tokens tokens = tokenService.issueTokens();

        String hashedToken = tokens.getHashedToken();

        assertThat(hashedToken, is(notNullValue()));
        assertThat(hashedToken.length(), is(60));
        assertThat(hashedToken, startsWith(EXPECTED_SALT));
    }

    @Test
    public void issueTokens_shouldIssueValidAPIKey() {

        Tokens tokens = tokenService.issueTokens();

        String apiKey = tokens.getApiKey();

        // Minimum length guarantee is 32 for Hmac and an extremely very unlikely
        // minimum value of 1 length for the random Token this value is more likely to be 25~26 chars length
        assertThat(apiKey.length(), is(greaterThan(33)));
        assertThat(containsAll(BASE32_HEX_DICTIONARY, asList(apiKey.toCharArray())), is(true));
    }

    @Test
    public void issueTokens_shouldIssueDifferentValuesWhenCallingTwice() {

        Tokens tokens1 = tokenService.issueTokens();
        Tokens tokens2 = tokenService.issueTokens();

        assertThat(tokens1.equals(tokens2), is(false));
    }

    @Test
    public void issueTokens_shouldIssueTokensThatMatchesWhenHashed() {

        Tokens tokens = tokenService.issueTokens();

        String apiKey = tokens.getApiKey();
        int hmacLength = 32;

        int tokenEnd = apiKey.length() - hmacLength;

        String plainToken = apiKey.substring(0, tokenEnd);

        assertThat(BCrypt.hashpw(plainToken, EXPECTED_SALT), is(tokens.getHashedToken()));
    }

    @Test
    public void issueTokens_shouldIssueApiKeyTokenWithHmacThatMatches() {

        Tokens tokens = tokenService.issueTokens();

        String apiKey = tokens.getApiKey();
        int hmacLength = 32;

        int tokenEnd = apiKey.length() - hmacLength;

        String plainToken = apiKey.substring(0, tokenEnd);
        String hmacApiKey = apiKey.substring(tokenEnd);

        String hmacFromExtractedPlainToken = BaseEncoding.base32Hex().omitPadding().lowerCase().encode(HmacUtils.hmacSha1(EXPECTED_SECRET_KEY, plainToken));

        assertThat(hmacFromExtractedPlainToken, is(hmacApiKey));
    }

    @Test
    public void extractEncryptedTokenFromApiKey_shouldNotBePresent_whenFormatIsNotValid() {

        Optional<String> tokenOptional = tokenService.extractEncryptedTokenFrom("a");
        assertThat(tokenOptional.isPresent(), is(false));
    }

    @Test
    public void extractEncryptedTokenFromApiKey_shouldNotBePresent_whenTokenDoesNotMatchHmac() {

        String token = "thisIsMyPlainToken";
        String hmac = BaseEncoding.base32Hex().omitPadding().lowerCase().encode(HmacUtils.hmacSha1(EXPECTED_SECRET_KEY, token));

        Optional<String> expectedValidTokenOptional = tokenService.extractEncryptedTokenFrom(token + hmac);
        assertThat(expectedValidTokenOptional.isPresent(), is(true));

        String tokenInvalid = token + "1";

        Optional<String> expectedInvalidTokenOptional = tokenService.extractEncryptedTokenFrom(tokenInvalid + hmac);
        assertThat(expectedInvalidTokenOptional.isPresent(), is(false));
    }
}
