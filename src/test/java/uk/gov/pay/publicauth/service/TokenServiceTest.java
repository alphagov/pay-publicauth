package uk.gov.pay.publicauth.service;

import com.google.common.io.BaseEncoding;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.publicauth.app.config.TokensConfiguration;
import uk.gov.pay.publicauth.auth.Token;
import uk.gov.pay.publicauth.dao.AuthTokenDao;
import uk.gov.pay.publicauth.exception.TokenInvalidException;
import uk.gov.pay.publicauth.exception.TokenRevokedException;
import uk.gov.pay.publicauth.model.AuthResponse;
import uk.gov.pay.publicauth.model.CreateTokenRequest;
import uk.gov.pay.publicauth.model.TokenEntity;
import uk.gov.pay.publicauth.model.TokenHash;
import uk.gov.pay.publicauth.model.TokenLink;

import java.util.List;
import java.util.Optional;

import static com.google.common.primitives.Chars.asList;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.pay.publicauth.fixture.TokenEntityFixture.aTokenEntity;
import static uk.gov.pay.publicauth.model.TokenPaymentType.CARD;
import static uk.gov.pay.publicauth.model.TokenSource.API;

@RunWith(MockitoJUnitRunner.class)
public class TokenServiceTest {

    private static final String EXPECTED_SALT = "$2a$10$IhaXo6LIBhKIWOiGpbtPOu";
    private static final String EXPECTED_SECRET_KEY = "qwer9yuhgf";
    private static final List<Character> BASE32_HEX_DICTIONARY = asList("0123456789abcdefghijklmnopqrstuv".toCharArray());
    private static final TokenHash TOKEN_HASH = TokenHash.of("TOKEN");

    private TokenService tokenService;

    @Mock
    TokensConfiguration mockConfig;

    @Mock
    AuthTokenDao mockAuthTokenDao;

    @Captor
    ArgumentCaptor<TokenHash> tokenHashArgumentCaptor;

    @Before
    public void setup() {
        when(mockConfig.getEncryptDBSalt()).thenReturn(EXPECTED_SALT);
        when(mockConfig.getApiKeyHmacSecret()).thenReturn(EXPECTED_SECRET_KEY);
        tokenService = new TokenService(mockConfig, mockAuthTokenDao);
    }

    @Test
    public void shouldSuccessfullyAuthenticateIfValidNotRevokedToken() {
        TokenEntity token = aTokenEntity().build();
        when(mockAuthTokenDao.findTokenByHash(TOKEN_HASH)).thenReturn(Optional.of(token));

        AuthResponse authResponse = tokenService.authenticate(TOKEN_HASH);

        verify(mockAuthTokenDao).updateLastUsedTime(TOKEN_HASH);

        assertThat(authResponse.getAccountId(), is(token.getAccountId()));
        assertThat(authResponse.getTokenLink(), is(token.getTokenLink()));
        assertThat(authResponse.getTokenPaymentType(), is(token.getTokenPaymentType()));
    }

    @Test
    public void shouldThrowExceptionIfTokenNotFound() {
        when(mockAuthTokenDao.findTokenByHash(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThrows(TokenInvalidException.class, () -> tokenService.authenticate(TOKEN_HASH));
        verify(mockAuthTokenDao, never()).updateLastUsedTime(TOKEN_HASH);
    }

    @Test
    public void shouldThrowExceptionIfTokenRevoked() {
        TokenEntity token = aTokenEntity()
                .withRevokedDate(now(UTC))
                .withTokenLink(TokenLink.of("a-token-link"))
                .build();
        when(mockAuthTokenDao.findTokenByHash(TOKEN_HASH)).thenReturn(Optional.of(token));

        assertThrows(TokenRevokedException.class, () -> tokenService.authenticate(TOKEN_HASH), "Token with token_link a-token-link has been revoked");
        verify(mockAuthTokenDao, never()).updateLastUsedTime(TOKEN_HASH);
    }

    @Test
    public void shouldCreateValidToken() {
        CreateTokenRequest createTokenRequest = new CreateTokenRequest("42", "A token description", "a-user-id", CARD, API);
        String apiKey = tokenService.createTokenForAccount(createTokenRequest);

        // Minimum length guarantee is 32 for Hmac and an extremely very unlikely
        // minimum value of 1 length for the random Token this value is more likely to be 24~26 chars length
        assertThat(apiKey.length(), is(greaterThan(33)));
        assertThat(BASE32_HEX_DICTIONARY.containsAll(asList(apiKey.toCharArray())), is(true));

        verify(mockAuthTokenDao).storeToken(tokenHashArgumentCaptor.capture(), eq(createTokenRequest));

        TokenHash hashedToken = tokenHashArgumentCaptor.getValue();
        assertThat(hashedToken, is(notNullValue()));
        assertThat(hashedToken.getValue().length(), is(60));
        assertThat(hashedToken.getValue(), startsWith(EXPECTED_SALT));

        int hmacLength = 32;
        int tokenEnd = apiKey.length() - hmacLength;
        String plainToken = apiKey.substring(0, tokenEnd);
        String hmacApiKey = apiKey.substring(tokenEnd);

        // check API key matched hashed token
        assertThat(BCrypt.hashpw(plainToken, EXPECTED_SALT), is(hashedToken.getValue()));

        // check Hmac matches token
        String hmacFromExtractedPlainToken = BaseEncoding.base32Hex().omitPadding().lowerCase().encode(new HmacUtils(HmacAlgorithms.HMAC_SHA_1, EXPECTED_SECRET_KEY).hmac(plainToken));
        assertThat(hmacFromExtractedPlainToken, is(hmacApiKey));
    }

    @Test
    public void shouldCreateDifferentTokensWhenCalledTwice() {
        CreateTokenRequest createTokenRequest = new CreateTokenRequest("42", "A token description", "a-user-id", CARD, API);
        String apiKey1 = tokenService.createTokenForAccount(createTokenRequest);
        String apiKey2 = tokenService.createTokenForAccount(createTokenRequest);

        assertThat(apiKey1.equals(apiKey2), is(false));
    }

    @Test
    public void extractEncryptedTokenFromApiKey_shouldNotBePresent_whenFormatIsNotValid() {

        Optional<Token> tokenOptional = tokenService.extractEncryptedTokenFrom("a");
        assertThat(tokenOptional.isPresent(), is(false));
    }

    @Test
    public void extractEncryptedTokenFromApiKey_shouldNotBePresent_whenTokenDoesNotMatchHmac() {

        String token = "thisismvplaintoken";
        String hmac = BaseEncoding.base32Hex().omitPadding().lowerCase().encode(new HmacUtils(HmacAlgorithms.HMAC_SHA_1, EXPECTED_SECRET_KEY).hmac(token));

        Optional<Token> expectedValidTokenOptional = tokenService.extractEncryptedTokenFrom(token + hmac);
        assertThat(expectedValidTokenOptional.isPresent(), is(true));

        String tokenInvalid = token + "1";

        Optional<Token> expectedInvalidTokenOptional = tokenService.extractEncryptedTokenFrom(tokenInvalid + hmac);
        assertThat(expectedInvalidTokenOptional.isPresent(), is(false));
    }

    @Test
    public void extractEncryptedTokenFromApiKey_shouldNotBePresent_whenLengthIsGreaterThanExpected() {

        String tokenGreaterThan26Characters = "morethan26chartokenisnotval";
        String hmac = BaseEncoding.base32Hex().omitPadding().lowerCase().encode(new HmacUtils(HmacAlgorithms.HMAC_SHA_1, EXPECTED_SECRET_KEY).hmac(tokenGreaterThan26Characters));

        Optional<Token> expectedValidTokenOptional = tokenService.extractEncryptedTokenFrom(tokenGreaterThan26Characters + hmac);
        assertThat(expectedValidTokenOptional.isPresent(), is(false));
    }

    @Test
    public void extractEncryptedTokenFromApiKey_shouldBePresent_evenWhenCharacterSetIsNotExpectedBase32HexLowercase_asLongTheHmacIsValid() {

        // Is more computationally expensive checking for character set validation than validating against the Hmac.
        // Enough to be a lightweight mechanism to check the token is genuine.

        String tokenLowercaseButNoInBase32Hex = "x";
        String hmac = BaseEncoding.base32Hex().omitPadding().lowerCase().encode(new HmacUtils(HmacAlgorithms.HMAC_SHA_1, EXPECTED_SECRET_KEY).hmac(tokenLowercaseButNoInBase32Hex));

        Optional<Token> expectedValidTokenOptional = tokenService.extractEncryptedTokenFrom(tokenLowercaseButNoInBase32Hex + hmac);
        assertThat(expectedValidTokenOptional.isPresent(), is(true));
    }

    @Test
    public void extractEncryptedTokenFromApiKey_shouldBePresent_evenWhenCharacterSetIsInBase32HexButUppercase_asLongTheHmacIsValid() {

        // Is more computationally expensive checking for character set validation than validating against the Hmac.
        // Enough to be a lightweight mechanism to check the token is genuine.

        String tokenUppercaseBase32Hex = "A";
        String hmac = BaseEncoding.base32Hex().omitPadding().lowerCase().encode(new HmacUtils(HmacAlgorithms.HMAC_SHA_1, EXPECTED_SECRET_KEY).hmac(tokenUppercaseBase32Hex));

        Optional<Token> expectedValidTokenOptional = tokenService.extractEncryptedTokenFrom(tokenUppercaseBase32Hex + hmac);
        assertThat(expectedValidTokenOptional.isPresent(), is(true));
    }
}
