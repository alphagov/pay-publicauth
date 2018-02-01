package uk.gov.pay.publicauth.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.gov.pay.publicauth.model.TokenPaymentType;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CreateTokenParserTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    CreateTokenParser createTokenParser = new CreateTokenParser();

    @Test
    public void shouldParseToken() throws IOException {
        JsonNode json = objectMapper.readTree("{"
                + "\"account_id\": \"12345\",\n"
                + "\"account_external_id\": \"abc123\",\n"
                + "\"token_type\": \"DIRECT_DEBIT\",\n"
                + "\"description\": \"Description\",\n"
                + "\"created_by\": \"Alex\"\n"
                + "}");

        CreateTokenParser.ParsedToken parsedToken = createTokenParser.parse(json);

        assertThat(parsedToken.getAccountId(), is("12345"));
        assertThat(parsedToken.getAccountExternalId(), is("abc123"));
        assertThat(parsedToken.getTokenPaymentType(), is(TokenPaymentType.DIRECT_DEBIT));
        assertThat(parsedToken.getDescription(), is("Description"));
        assertThat(parsedToken.getCreatedBy(), is("Alex"));
    }

    @Test
    public void shouldSetTokenTypeToCardIfNotIncluded() throws IOException {
        JsonNode json = objectMapper.readTree("{"
                + "\"account_id\": \"12345\",\n"
                + "\"account_external_id\": \"abc123\",\n"
                + "\"description\": \"Description\",\n"
                + "\"created_by\": \"Alex\"\n"
                + "}");

        CreateTokenParser.ParsedToken parsedToken = createTokenParser.parse(json);

        assertThat(parsedToken.getTokenPaymentType(), is(TokenPaymentType.CARD));
    }

    @Test
    public void shouldNotHaveAccountExternalIdIfNotIncluded() throws IOException {
        JsonNode json = objectMapper.readTree("{"
                + "\"account_id\": \"12345\",\n"
                + "\"token_type\": \"DIRECT_DEBIT\",\n"
                + "\"description\": \"Description\",\n"
                + "\"created_by\": \"Alex\"\n"
                + "}");

        CreateTokenParser.ParsedToken parsedToken = createTokenParser.parse(json);

        assertThat(parsedToken.getAccountExternalId(), is(nullValue()));
    }

    @Test
    public void shouldNotHaveAccountExternalIdIfSentAsEmptyString() throws IOException {
        JsonNode json = objectMapper.readTree("{"
                + "\"account_id\": \"12345\",\n"
                + "\"account_external_id\": \"\",\n"
                + "\"token_type\": \"DIRECT_DEBIT\",\n"
                + "\"description\": \"Description\",\n"
                + "\"created_by\": \"Alex\"\n"
                + "}");

        CreateTokenParser.ParsedToken parsedToken = createTokenParser.parse(json);

        assertThat(parsedToken.getAccountExternalId(), is(nullValue()));
    }

}
