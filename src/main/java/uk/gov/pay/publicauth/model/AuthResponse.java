package uk.gov.pay.publicauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

public class AuthResponse {

    @JsonProperty("account_id")
    @Schema(example = "1234")
    private String accountId;

    @JsonProperty("token_link")
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000", implementation = String.class)
    @JsonSerialize(using = ToStringSerializer.class)
    private TokenLink tokenLink;

    @JsonProperty("token_type")
    @Schema(example = "CARD")
    @JsonSerialize(using = ToStringSerializer.class)
    private TokenPaymentType tokenPaymentType;

    @JsonProperty("service_mode")
    @Schema(example = "LIVE")
    private final ServiceMode serviceMode;

    @JsonProperty("service_external_id")
    @Schema(example = "cd1b871207a94a7fa157dee678146acd")
    private final String serviceExternalId;
         
    public AuthResponse(TokenEntity tokenEntity) {
        this.accountId = tokenEntity.getAccountId();
        this.tokenLink = tokenEntity.getTokenLink();
        this.tokenPaymentType = tokenEntity.getTokenPaymentType();
        this.serviceMode = tokenEntity.getServiceMode();
        this.serviceExternalId = tokenEntity.getServiceExternalId();
    }

    public String getAccountId() {
        return accountId;
    }

    public TokenLink getTokenLink() {
        return tokenLink;
    }

    public TokenPaymentType getTokenPaymentType() {
        return tokenPaymentType;
    }
    
    public ServiceMode getServiceMode() {
        return serviceMode;
    }
    
    public String getServiceExternalId() {
        return serviceExternalId;
    }
}
