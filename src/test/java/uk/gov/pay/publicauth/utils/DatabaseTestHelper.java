package uk.gov.pay.publicauth.utils;

import org.jdbi.v3.core.Jdbi;
import uk.gov.pay.publicauth.model.TokenHash;
import uk.gov.pay.publicauth.model.TokenLink;
import uk.gov.pay.publicauth.model.TokenPaymentType;
import uk.gov.pay.publicauth.model.TokenSource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static java.sql.Timestamp.from;
import static java.time.ZoneOffset.UTC;
import static uk.gov.pay.publicauth.model.TokenPaymentType.CARD;
import static uk.gov.pay.publicauth.model.TokenSource.API;

public class DatabaseTestHelper {

    private final Jdbi jdbi;

    DatabaseTestHelper(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public void insertAccount(TokenHash tokenHash, TokenLink randomTokenLink, String accountId, String description, String createdBy) {
        insertAccount(tokenHash, randomTokenLink, accountId, description, null, createdBy, ZonedDateTime.now(UTC));
    }

    public void insertAccount(TokenHash tokenHash, TokenLink randomTokenLink, String accountId, String description, ZonedDateTime revoked, String createdBy) {
        insertAccount(tokenHash, randomTokenLink, accountId, description, revoked, createdBy, ZonedDateTime.now(UTC));
    }

    public void insertAccount(TokenHash tokenHash, TokenLink randomTokenLink, String accountId, String description, ZonedDateTime revoked, String createdBy, ZonedDateTime lastUsed) {
        insertAccount(tokenHash, randomTokenLink, API, accountId, description, revoked, createdBy, lastUsed, CARD);
    }

    public void insertAccount(TokenHash tokenHash, TokenLink randomTokenLink, TokenSource tokenSource, String accountId, String description, ZonedDateTime revoked, String createdBy, ZonedDateTime lastUsed) {
        insertAccount(tokenHash, randomTokenLink, tokenSource, accountId, description, revoked, createdBy, lastUsed, CARD);
    }

    public void insertAccount(TokenHash tokenHash, TokenLink randomTokenLink, TokenSource tokenSource, String accountId, String description, ZonedDateTime revoked, String createdBy, ZonedDateTime lastUsed, TokenPaymentType tokenPaymentType) {
        jdbi.withHandle(handle ->
                handle.createUpdate("INSERT INTO tokens(token_hash, token_link, type, account_id, description, token_type, revoked, created_by, last_used) " +
                                "VALUES (:token_hash,:token_link,:type,:account_id,:description,:token_type,(:revoked at time zone 'utc'), :created_by, (:last_used at time zone 'utc'))")
                        .bind("token_hash", tokenHash.getValue())
                        .bind("token_link", randomTokenLink.toString())
                        .bind("type", tokenSource)
                        .bind("account_id", accountId)
                        .bind("description", description)
                        .bind("token_type", tokenPaymentType)
                        .bind("revoked", revoked == null ? null : from(revoked.toInstant()))
                        .bind("created_by", createdBy)
                        .bind("last_used", lastUsed == null ? null : from(lastUsed.toInstant()))
                        .execute());
    }

    public ZonedDateTime issueTimestampForAccount(String accountId) {
        return getDateTimeColumn("issued", accountId);
    }

    public java.util.Optional<String> lookupColumnForTokenTable(String column, String idKey, String idValue) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT " + column + " FROM tokens WHERE " + idKey + "=:placeholder")
                        .bind("placeholder", idValue)
                        .mapTo(String.class)
                        .findFirst());
    }

    public Map<String, Object> getTokenByHash(TokenHash tokenHash) {
        return jdbi.withHandle(h ->
                h.createQuery("SELECT token_id, type, token_type, token_hash, account_id, issued, revoked, token_link, description, created_by, service_mode, service_external_id " +
                                "FROM tokens t " +
                                "WHERE token_hash = :token_hash")
                        .bind("token_hash", tokenHash.getValue())
                        .mapToMap()
                        .first());
    }

    public ZonedDateTime getDateTimeColumn(String column, String accountId) {
        return jdbi.withHandle(handle ->
                        handle.createQuery("SELECT " + column + " FROM tokens WHERE account_id=:accountId")
                                .bind("accountId", accountId)
                                .mapTo(LocalDateTime.class)
                                .first())
                .atZone(ZoneId.of("UTC"));
    }

    public ZonedDateTime getCurrentTime() {
        return jdbi.withHandle(handle ->
                        handle.createQuery("SELECT (now() at time zone 'utc')")
                                .mapTo(LocalDateTime.class)
                                .first())
                .atZone(ZoneId.of("UTC"));

    }

    public void truncateDatabase() {
        jdbi.withHandle(handle -> handle.execute("TRUNCATE tokens"));
    }
}
