package uk.gov.pay.publicauth.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import uk.gov.pay.publicauth.model.TokenPaymentType;

import java.util.Optional;

import static java.util.UUID.randomUUID;
import static uk.gov.pay.publicauth.model.TokenPaymentType.CARD;
import static uk.gov.pay.publicauth.model.TokenPaymentType.valueOf;

class CreateTokenParser {
    private static final String ACCOUNT_ID_FIELD = "account_id";
    private static final String ACCOUNT_EXTERNAL_ID_FIELD = "account_external_id";
    private static final String TOKEN_TYPE_FIELD = "token_type";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String CREATED_BY_FIELD = "created_by";

    public static class ParsedToken {
        private String tokenLink;
        private String accountId;
        private String accountExternalId;
        private String description;
        private String createdBy;
        private TokenPaymentType tokenPaymentType;

        ParsedToken(String tokenLink, String accountId, String accountExternalId, String description, String createdBy, TokenPaymentType tokenPaymentType) {
            this.tokenLink = tokenLink;
            this.accountId = accountId;
            this.accountExternalId = accountExternalId;
            this.description = description;
            this.createdBy = createdBy;
            this.tokenPaymentType = tokenPaymentType;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getAccountExternalId() {
            return accountExternalId;
        }

        public String getDescription() {
            return description;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public TokenPaymentType getTokenPaymentType() {
            return tokenPaymentType;
        }

        public String getTokenLink() {
            return tokenLink;
        }
    }
    public ParsedToken parse(JsonNode payload) {
        String tokenLink = randomUUID().toString();

        //todo check if this can be removed after updating selfservice and scripts
        TokenPaymentType tokenPaymentType =
                Optional.ofNullable(payload.get(TOKEN_TYPE_FIELD))
                        .map(tokenType -> valueOf(tokenType.asText()))
                        .orElse(CARD);

        String accountExternalId =
                Optional.ofNullable(payload.get(ACCOUNT_EXTERNAL_ID_FIELD))
                        .map(JsonNode::asText)
                        .filter(StringUtils::isNotBlank)
                        .orElse(null);

        return new ParsedToken(
                tokenLink,
                payload.get(ACCOUNT_ID_FIELD).asText(),
                accountExternalId,
                payload.get(DESCRIPTION_FIELD).asText(),
                payload.get(CREATED_BY_FIELD).asText(), tokenPaymentType);
    }
}
