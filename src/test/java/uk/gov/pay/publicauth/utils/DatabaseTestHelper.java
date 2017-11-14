package uk.gov.pay.publicauth.utils;

import io.dropwizard.jdbi.args.JodaDateTimeMapper;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.util.StringMapper;
import uk.gov.pay.publicauth.model.TokenPaymentType;

import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

public class DatabaseTestHelper {
    private DBI jdbi;

    public DatabaseTestHelper(DBI jdbi) {
        this.jdbi = jdbi;
    }

    public void insertAccount(String tokenHash, String randomTokenLink, String accountId, String description, String createdBy) {
        insertAccount(tokenHash, randomTokenLink, accountId, description, null, createdBy, DateTime.now(), TokenPaymentType.CARD);
    }

    public void insertAccount(String tokenHash, String randomTokenLink, String accountId, String description, DateTime revoked, String createdBy) {
        insertAccount(tokenHash, randomTokenLink, accountId, description, revoked, createdBy, DateTime.now(), TokenPaymentType.CARD);
    }

    public void insertAccount(String tokenHash, String randomTokenLink, String accountId, String description, DateTime revoked, String createdBy, DateTime lastUsed) {
        insertAccount(tokenHash, randomTokenLink, accountId, description, revoked, createdBy, lastUsed, TokenPaymentType.CARD);
    }

    public void insertAccount(String tokenHash, String randomTokenLink, String accountId, String description, DateTime revoked, String createdBy, DateTime lastUsed, TokenPaymentType tokenPaymentType) {
            jdbi.withHandle(handle ->
                    handle.insert("INSERT INTO tokens(token_hash, token_link, account_id, description, token_type, revoked, created_by, last_used) VALUES (?,?,?,?,?,(? at time zone 'utc'),?,(? at time zone 'utc'))",
                            tokenHash, randomTokenLink, accountId, description, tokenPaymentType.toString(), revoked, createdBy, lastUsed));
    }

    public DateTime issueTimestampForAccount(String accountId) {
        return getDateTimeColumn("issued", accountId);
    }

    public DateTime revokeTimestampForAccount(String accountId) {
        return getDateTimeColumn("revoked", accountId);
    }

    public java.util.Optional<String> lookupColumnForTokenTable(String column, String idKey, String idValue) {
        return java.util.Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("SELECT " + column + " FROM tokens WHERE " + idKey + "=:placeholder")
                        .bind("placeholder", idValue)
                        .map(StringMapper.FIRST)
                        .first()));

    }

    public Map<String, Object> getTokenByHash(String tokenHash) {
        Map<String, Object> ret = jdbi.withHandle(h ->
                h.createQuery("SELECT token_id, token_hash, account_id, token_type, issued, revoked, token_link, description, created_by " +
                        "FROM tokens t " +
                        "WHERE token_hash = :token_hash")
                        .bind("token_hash", tokenHash)
                        .first());
        return ret;
    }

    public DateTime getDateTimeColumn(String column, String accountId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT " + column + " FROM tokens WHERE account_id=:accountId")
                        .bind("accountId", accountId)
                        .map(new JodaDateTimeMapper(Optional.of(TimeZone.getTimeZone("UTC"))))
                        .first());
    }

    public DateTime getCurrentTime() {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT (now() at time zone 'utc')")
                        .map(new JodaDateTimeMapper(Optional.of(TimeZone.getTimeZone("UTC"))))
                        .first());
    }

}
