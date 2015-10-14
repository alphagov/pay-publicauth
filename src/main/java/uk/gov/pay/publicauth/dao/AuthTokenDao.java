package uk.gov.pay.publicauth.dao;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.util.StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AuthTokenDao {
    private DBI jdbi;
    public static Logger log = LoggerFactory.getLogger(AuthTokenDao.class);

    public AuthTokenDao(DBI jdbi) {
        this.jdbi = jdbi;
    }


    public Optional<String> findAccount(String tokenHash) {
        return Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("SELECT account_id FROM tokens WHERE token_hash = :token_hash and revoked IS NULL")
                        .bind("token_hash", tokenHash)
                        .map(StringMapper.FIRST)
                        .first()));
    }

    public Optional<String> findDescription(String tokenHash) {
        return Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("SELECT description FROM tokens WHERE token_hash = :token_hash and revoked IS NULL")
                        .bind("token_hash", tokenHash)
                        .map(StringMapper.FIRST)
                        .first()));
    }

    public void storeToken(String tokenHash, String accountId, String description) {
        Integer rowsUpdated = jdbi.withHandle(handle ->
                        handle.insert("INSERT INTO tokens(token_hash, description, account_id) VALUES (?,?,?)", tokenHash, description, accountId)
        );
        if (rowsUpdated != 1) {
            log.error("Unable to store new token for account {}", accountId);
            throw new RuntimeException(String.format("Unable to store new token for account %s}", accountId));
        }
    }

    public boolean revokeToken(String accountId) {
        int rowsUpdated = jdbi.withHandle(handle ->
            handle.update("UPDATE tokens SET revoked=(now() at time zone 'utc') WHERE account_id=? AND revoked IS NULL", accountId)
        );
        return rowsUpdated > 0;
    }
}
