package uk.gov.pay.publicauth.fixture;

import uk.gov.pay.publicauth.model.TokenEntity;
import uk.gov.pay.publicauth.model.TokenLink;
import uk.gov.pay.publicauth.model.TokenPaymentType;
import uk.gov.pay.publicauth.model.TokenSource;

import java.time.ZonedDateTime;

import static uk.gov.pay.publicauth.model.TokenPaymentType.CARD;
import static uk.gov.pay.publicauth.model.TokenSource.API;

public class TokenEntityFixture {
    private TokenLink tokenLink = TokenLink.of("123456789101112131415161718192021222");
    private String description = "token description";
    private String accountId = "42";
    private TokenPaymentType tokenPaymentType = CARD;
    private TokenSource tokenSource = API;
    private ZonedDateTime revokedDate;
    private ZonedDateTime issuedDate = ZonedDateTime.parse("2020-01-01T12:30:00.000Z");
    private ZonedDateTime lastUsedDate;
    private String createdBy = "a-user-id";

    private TokenEntityFixture() {
    }
    
    public static TokenEntityFixture aTokenEntity() {
        return new TokenEntityFixture();
    }

    public TokenEntityFixture withTokenLink(TokenLink tokenLink) {
        this.tokenLink = tokenLink;
        return this;
    }

    public TokenEntityFixture withDescription(String description) {
        this.description = description;
        return this;
    }

    public TokenEntityFixture withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public TokenEntityFixture withTokenPaymentType(TokenPaymentType tokenPaymentType) {
        this.tokenPaymentType = tokenPaymentType;
        return this;
    }

    public TokenEntityFixture withTokenSource(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
        return this;
    }

    public TokenEntityFixture withRevokedDate(ZonedDateTime revokedDate) {
        this.revokedDate = revokedDate;
        return this;
    }

    public TokenEntityFixture withIssuedDate(ZonedDateTime issuedDate) {
        this.issuedDate = issuedDate;
        return this;
    }

    public TokenEntityFixture withLastUsedDate(ZonedDateTime lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
        return this;
    }

    public TokenEntityFixture withCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public TokenEntity build() {
        return new TokenEntity(
                this.tokenLink,
                this.description,
                this.accountId,
                this.tokenPaymentType,
                this.tokenSource,
                this.revokedDate,
                this.issuedDate,
                this.lastUsedDate,
                this.createdBy);
    }
}
