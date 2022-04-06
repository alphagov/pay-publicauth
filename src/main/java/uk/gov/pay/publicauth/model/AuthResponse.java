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

    public AuthResponse(String accountId, TokenLink tokenLink, TokenPaymentType tokenPaymentType) {
        this.tokenLink = tokenLink;
        this.accountId = accountId;
        this.tokenPaymentType = tokenPaymentType;
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
}
