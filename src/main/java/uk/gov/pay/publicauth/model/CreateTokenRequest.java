package uk.gov.pay.publicauth.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
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
    @Schema(hidden = true)
    @NotNull
    private final ServiceMode serviceMode;
    @Schema(hidden = true)
    @NotNull
    private final String serviceExternalId;

    @JsonCreator
    public CreateTokenRequest(@Schema(example = "1", description = "Gateway account to associate the new token to", requiredMode = REQUIRED)
                              @NotNull @JsonProperty("account_id") String accountId,
                              @Schema(example = "Token description", description = "Description of the new token", requiredMode = REQUIRED)
                              @NotNull @JsonProperty("description") String description,
                              @Schema(example = "test@example.org", requiredMode = REQUIRED) @NotNull @JsonProperty("created_by") String createdBy,
                              @Schema(example = "CARD", defaultValue = "CARD")
                              @JsonProperty("token_type") TokenPaymentType tokenPaymentType,
                              @Schema(example = "API", defaultValue = "API")
                              @JsonProperty("type") TokenSource tokenSource,
                              @Schema(example = "LIVE", defaultValue = "LIVE")
                              @JsonProperty("token_account_type") TokenAccountType tokenAccountType,
                              @Schema(example = "LIVE", requiredMode = REQUIRED)
                              @JsonProperty("service_mode") ServiceMode serviceMode,
                              @Schema(example = "cd1b871207a94a7fa157dee678146acd", requiredMode = REQUIRED)
                              @JsonProperty("service_external_id") String serviceExternalId
        
    ) {
        this.accountId = accountId;
        this.description = description;
        this.createdBy = createdBy;
        this.tokenPaymentType = tokenPaymentType == null ? CARD : tokenPaymentType;
        this.tokenSource = tokenSource == null ? API : tokenSource;
        this.tokenAccountType = tokenAccountType;
        this.serviceMode = serviceMode;
        this.serviceExternalId = serviceExternalId;
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
    
    public ServiceMode getServiceMode() {
        return serviceMode;
    }
    
    public String getServiceExternalId() {
        return serviceExternalId;
    }
}
