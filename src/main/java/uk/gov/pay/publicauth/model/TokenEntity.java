package uk.gov.pay.publicauth.model;

import java.time.ZonedDateTime;

public class TokenEntity {

    private TokenLink tokenLink;
    private String description;
    private String accountId;
    private TokenPaymentType tokenPaymentType;
    private TokenSource tokenSource;
    private ZonedDateTime revokedDate;
    private ZonedDateTime issuedDate;
    private ZonedDateTime lastUsedDate;
    private String createdBy;

    public TokenEntity(TokenLink tokenLink,
                       String description,
                       String accountId,
                       TokenPaymentType tokenPaymentType,
                       TokenSource tokenSource,
                       ZonedDateTime revokedDate,
                       ZonedDateTime issuedDate,
                       ZonedDateTime lastUsedDate,
                       String createdBy) {
        this.tokenLink = tokenLink;
        this.description = description;
        this.accountId = accountId;
        this.tokenPaymentType = tokenPaymentType;
        this.tokenSource = tokenSource;
        this.revokedDate = revokedDate;
        this.issuedDate = issuedDate;
        this.lastUsedDate = lastUsedDate;
        this.createdBy = createdBy;
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

        public Builder() {
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

        public TokenEntity build() {
            return new TokenEntity(this);
        }
    }
}
