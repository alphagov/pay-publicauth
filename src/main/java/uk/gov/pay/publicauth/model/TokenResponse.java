package uk.gov.pay.publicauth.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.publicauth.json.DateTimeStringSerializer;

import java.time.ZonedDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {

    @JsonProperty("token_link")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000", implementation = String.class)
    private TokenLink tokenLink;

    @JsonProperty("description")
    @Schema(example = "Description of the token")
    private String description;

    @JsonProperty("token_type")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(example = "CARD")
    private TokenPaymentType tokenPaymentType;

    @JsonProperty("type")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(example = "API")
    private TokenSource tokenSource;

    @JsonProperty("revoked")
    @JsonSerialize(using = DateTimeStringSerializer.class)
    @Schema(example = "05 Apr 2022 - 20:02", implementation = String.class)
    private ZonedDateTime revokedDate;

    @JsonProperty("issued_date")
    @Schema(example = "04 Apr 2022 - 19:02", implementation = String.class)
    @JsonSerialize(using = DateTimeStringSerializer.class)
    private ZonedDateTime issuedDate;

    @JsonProperty("last_used")
    @Schema(example = "05 Apr 2022 - 19:02", implementation = String.class)
    @JsonSerialize(using = DateTimeStringSerializer.class)
    private ZonedDateTime lastUsedDate;

    @JsonProperty("created_by")
    @Schema(example = "test@example.org")
    private String createdBy;

    @JsonProperty("service_mode")
    @Schema(example = "live")
    private ServiceMode serviceMode;

    @JsonProperty("service_external_id")
    @Schema(example = "cd1b871207a94a7fa157dee678146acd")
    private String serviceExternalId;

    public TokenResponse(TokenLink tokenLink,
                         String description,
                         TokenPaymentType tokenPaymentType,
                         TokenSource tokenSource,
                         ZonedDateTime revokedDate,
                         ZonedDateTime issuedDate,
                         ZonedDateTime lastUsedDate,
                         String createdBy,
                         ServiceMode serviceMode,
                         String serviceExternalId) {
        this.tokenLink = tokenLink;
        this.description = description;
        this.tokenPaymentType = tokenPaymentType;
        this.tokenSource = tokenSource;
        this.revokedDate = revokedDate;
        this.issuedDate = issuedDate;
        this.lastUsedDate = lastUsedDate;
        this.createdBy = createdBy;
        this.serviceMode = serviceMode;
        this.serviceExternalId = serviceExternalId;
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
                tokenEntity.getCreatedBy(),
                tokenEntity.getServiceMode(),
                tokenEntity.getServiceExternalId()
        );
    }
}
