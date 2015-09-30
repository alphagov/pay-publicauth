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

    public void insertAccount(String bearerToken, String accountId) {
        jdbi.withHandle(handle -> handle.insert("INSERT INTO tokens(token_hash, account_id) VALUES (?, ?)", bearerToken, accountId));
    }

    public DateTime issueTimestampForAccount(String accountId) {
        return getDateTimeColumn("issued", accountId);
    }

    public DateTime revokeTimestampForAccount(String accountId) {
        return getDateTimeColumn("revoked", accountId);
    }

    public String lookupTokenFor(String accountId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT token_hash FROM tokens WHERE account_id=:accountId")
                        .bind("accountId", accountId)
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
}
