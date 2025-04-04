package uk.gov.pay.publicauth.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import static java.util.UUID.randomUUID;
import static uk.gov.pay.publicauth.model.TokenPaymentType.CARD;
import static uk.gov.pay.publicauth.model.TokenSource.API;

public class CreateTokenRequest {

    @Schema(hidden = true)
    @NotNull
    private final String accountId;
    @NotNull
    private final String description;
    @Schema(hidden = true)
    @NotNull
    private final String createdBy;
    @Schema(hidden = true)
    private final TokenPaymentType tokenPaymentType;
    @Schema(hidden = true)
    private final TokenSource tokenSource;
    @Schema(hidden = true)
    private final TokenLink tokenLink = TokenLink.of(randomUUID().toString());
    @Schema(hidden = true)
    private final TokenAccountType tokenAccountType;

    @JsonCreator
    public CreateTokenRequest(@Schema(example = "1", required = true, description = "Gateway account to associate the new token to")
                              @NotNull @JsonProperty("account_id") String accountId,
                              @Schema(example = "Token description", required = true, description = "Description of the new token")
                              @NotNull @JsonProperty("description") String description,
                              @Schema(required = true, example = "test@example.org") @NotNull @JsonProperty("created_by") String createdBy,
                              @Schema(example = "CARD", defaultValue = "CARD")
                              @JsonProperty("token_type") TokenPaymentType tokenPaymentType,
                              @Schema(example = "API", defaultValue = "API")
                              @JsonProperty("type") TokenSource tokenSource,
                              @Schema(example = "LIVE", defaultValue = "LIVE")
                              @JsonProperty("token_account_type") TokenAccountType tokenAccountType) {
        this.accountId = accountId;
        this.description = description;
        this.createdBy = createdBy;
        this.tokenPaymentType = tokenPaymentType == null ? CARD : tokenPaymentType;
        this.tokenSource = tokenSource == null ? API : tokenSource;
        this.tokenAccountType = tokenAccountType;
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

    public TokenAccountType getTokenAccountType() {
        return tokenAccountType;
    }
}
