package uk.gov.pay.publicauth.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class AuthResponse {
    
    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("token_link")
    @JsonSerialize(using = ToStringSerializer.class)
    private TokenLink tokenLink;
    
    @JsonProperty("token_type")
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
