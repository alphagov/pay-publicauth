package uk.gov.pay.publicauth.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import uk.gov.pay.publicauth.json.DateTimeStringSerializer;

import java.time.ZonedDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {
    
    @JsonProperty("token_link")
    @JsonSerialize(using = ToStringSerializer.class)
    private TokenLink tokenLink;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("token_type")
    @JsonSerialize(using = ToStringSerializer.class)
    private TokenPaymentType tokenPaymentType;
    
    @JsonProperty("type")
    @JsonSerialize(using = ToStringSerializer.class)
    private TokenSource tokenSource;
    
    @JsonProperty("revoked")
    @JsonSerialize(using = DateTimeStringSerializer.class)
    private ZonedDateTime revokedDate;
    
    @JsonProperty("issued_date")
    @JsonSerialize(using = DateTimeStringSerializer.class)
    private ZonedDateTime issuedDate;
    
    @JsonProperty("last_used")
    @JsonSerialize(using = DateTimeStringSerializer.class)
    private ZonedDateTime lastUsedDate;
    
    @JsonProperty("created_by")
    private String createdBy;

    public TokenResponse(TokenLink tokenLink,
                         String description,
                         TokenPaymentType tokenPaymentType,
                         TokenSource tokenSource,
                         ZonedDateTime revokedDate,
                         ZonedDateTime issuedDate,
                         ZonedDateTime lastUsedDate,
                         String createdBy) {
        this.tokenLink = tokenLink;
        this.description = description;
        this.tokenPaymentType = tokenPaymentType;
        this.tokenSource = tokenSource;
        this.revokedDate = revokedDate;
        this.issuedDate = issuedDate;
        this.lastUsedDate = lastUsedDate;
        this.createdBy = createdBy;
    }
    
    public static TokenResponse fromEntity(TokenEntity tokenEntity) {
        return new TokenResponse(
                tokenEntity.getTokenLink(),
                tokenEntity.getDescription(),
                tokenEntity.getTokenPaymentType(),
                tokenEntity.getTokenSource(),
                tokenEntity.getRevokedDate(),
                tokenEntity.getIssuedDate(),
                tokenEntity.getLastUsedDate(),
                tokenEntity.getCreatedBy()
        );
    }
}
