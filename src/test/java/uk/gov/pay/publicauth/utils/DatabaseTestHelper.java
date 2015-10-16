package uk.gov.pay.publicauth.utils;

import com.google.common.base.Optional;
import io.dropwizard.jdbi.args.JodaDateTimeMapper;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.util.StringMapper;

import java.util.TimeZone;

public class DatabaseTestHelper {
    private DBI jdbi;

    public DatabaseTestHelper(DBI jdbi) {
        this.jdbi = jdbi;
    }

    public void insertAccount(String tokenHash, String randomTokenLink, String accountId, String description) {
        insertAccount(tokenHash, randomTokenLink, accountId, description, false);
    }

    public void insertAccount(String tokenHash, String randomTokenLink, String accountId, String description, Boolean revoked) {
        if (revoked) {
            jdbi.withHandle(handle -> handle.insert("INSERT INTO tokens(token_hash, token_link, account_id, description, revoked) VALUES (?,?,?,?,(now() at time zone 'utc'))", tokenHash, randomTokenLink, accountId, description));
        } else {
            jdbi.withHandle(handle -> handle.insert("INSERT INTO tokens(token_hash, token_link, account_id, description) VALUES (?,?,?,?)", tokenHash, randomTokenLink, accountId, description));
        }
    }

    public DateTime issueTimestampForAccount(String accountId) {
        return getDateTimeColumn("issued", accountId);
    }

    public DateTime revokeTimestampForAccount(String accountId) {
        return getDateTimeColumn("revoked", accountId);
    }

    public String lookupColumnFor(String column, String idKey, String idValue) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT " + column + " FROM tokens WHERE " + idKey + "=:placeholder")
                        .bind("placeholder", idValue)
                        .map(StringMapper.FIRST)
                        .first());

    }

    private DateTime getDateTimeColumn(String column, String accountId) {
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
