package uk.gov.pay.publicauth.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

import static java.util.UUID.randomUUID;
import static uk.gov.pay.publicauth.model.TokenPaymentType.CARD;
import static uk.gov.pay.publicauth.model.TokenSource.API;

public class CreateTokenRequest {

    @NotNull private final String accountId;
    @NotNull private final String description;
    @NotNull private final String createdBy;
    private final TokenPaymentType tokenPaymentType;
    private final TokenSource tokenSource;
    private final TokenLink tokenLink = TokenLink.of(randomUUID().toString());

    @JsonCreator
    public CreateTokenRequest(@JsonProperty("account_id") String accountId,
                              @JsonProperty("description") String description,
                              @JsonProperty("created_by") String createdBy,
                              @JsonProperty("token_type") TokenPaymentType tokenPaymentType,
                              @JsonProperty("type") TokenSource tokenSource) {
        this.accountId = accountId;
        this.description = description;
        this.createdBy = createdBy;
        this.tokenPaymentType = tokenPaymentType == null ? CARD : tokenPaymentType;
        this.tokenSource = tokenSource == null ? API : tokenSource;
    }

    public String getAccountId() {
        return accountId;
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

    public TokenSource getTokenSource() {
        return tokenSource;
    }

    public TokenLink getTokenLink() {
        return tokenLink;
    }
}
