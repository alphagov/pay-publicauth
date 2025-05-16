package uk.gov.pay.publicauth.model;

import java.time.ZonedDateTime;

public class TokenEntity {
    private final TokenLink tokenLink;
    private final String description;
    private final String accountId;
    private final TokenPaymentType tokenPaymentType;
    private final TokenSource tokenSource;
    private final ZonedDateTime revokedDate;
    private final ZonedDateTime issuedDate;
    private final ZonedDateTime lastUsedDate;
    private final String createdBy;
    private final ServiceMode serviceMode;
    private final String serviceExternalId;

    public TokenEntity(TokenLink tokenLink,
                       String description,
                       String accountId,
                       TokenPaymentType tokenPaymentType,
                       TokenSource tokenSource,
                       ZonedDateTime revokedDate,
                       ZonedDateTime issuedDate,
                       ZonedDateTime lastUsedDate,
                       String createdBy,
                       ServiceMode serviceMode,
                       String serviceExternalId
    ) {
        this.tokenLink = tokenLink;
        this.description = description;
        this.accountId = accountId;
        this.tokenPaymentType = tokenPaymentType;
        this.tokenSource = tokenSource;
        this.revokedDate = revokedDate;
        this.issuedDate = issuedDate;
        this.lastUsedDate = lastUsedDate;
        this.createdBy = createdBy;
        this.serviceMode = serviceMode;
        this.serviceExternalId = serviceExternalId;
    }

    public TokenEntity(Builder builder) {
        this.tokenLink = builder.tokenLink;
        this.description = builder.description;
        this.accountId = builder.accountId;
        this.tokenPaymentType = builder.tokenPaymentType;
        this.tokenSource = builder.tokenSource;
        this.revokedDate = builder.revokedDate;
        this.issuedDate = builder.issuedDate;
        this.lastUsedDate = builder.lastUsedDate;
        this.createdBy = builder.createdBy;
        this.serviceMode = builder.serviceMode;
        this.serviceExternalId = builder.serviceExternalId;
    }

    public TokenLink getTokenLink() {
        return tokenLink;
    }

    public String getDescription() {
        return description;
    }

    public String getAccountId() {
        return accountId;
    }

    public TokenPaymentType getTokenPaymentType() {
        return tokenPaymentType;
    }

    public TokenSource getTokenSource() {
        return tokenSource;
    }

    public ZonedDateTime getRevokedDate() {
        return revokedDate;
    }

    public ZonedDateTime getIssuedDate() {
        return issuedDate;
    }

    public ZonedDateTime getLastUsedDate() {
        return lastUsedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public ServiceMode getServiceMode() {
        return serviceMode;
    }

    public String getServiceExternalId() {
        return serviceExternalId;
    }

    public static final class Builder {
        private TokenLink tokenLink;
        private String description;
        private String accountId;
        private TokenPaymentType tokenPaymentType;
        private TokenSource tokenSource;
        private ZonedDateTime revokedDate;
        private ZonedDateTime issuedDate;
        private ZonedDateTime lastUsedDate;
        private String createdBy;
        private ServiceMode serviceMode;
        private String serviceExternalId;

        public Builder() {
            /* empty */
        }

        public Builder withTokenLink(TokenLink tokenLink) {
            this.tokenLink = tokenLink;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withTokenPaymentType(TokenPaymentType tokenPaymentType) {
            this.tokenPaymentType = tokenPaymentType;
            return this;
        }

        public Builder withTokenSource(TokenSource tokenSource) {
            this.tokenSource = tokenSource;
            return this;
        }

        public Builder withRevokedDate(ZonedDateTime revokedDate) {
            this.revokedDate = revokedDate;
            return this;
        }

        public Builder withIssuedDate(ZonedDateTime issuedDate) {
            this.issuedDate = issuedDate;
            return this;
        }

        public Builder withLastUsedDate(ZonedDateTime lastUsedDate) {
            this.lastUsedDate = lastUsedDate;
            return this;
        }

        public Builder withCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        
        public Builder withServiceMode(ServiceMode serviceMode) {
            this.serviceMode = serviceMode;
            return this;
        }

        public Builder withServiceExternalId(String serviceExternalId) {
            this.serviceExternalId = serviceExternalId;
            return this;
        }

        public TokenEntity build() {
            return new TokenEntity(this);
        }
    }
}
