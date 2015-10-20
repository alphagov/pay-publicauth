package uk.gov.pay.publicauth.dao;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.util.StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AuthTokenDao {
    private DBI jdbi;
    public static Logger log = LoggerFactory.getLogger(AuthTokenDao.class);

    public AuthTokenDao(DBI jdbi) {
        this.jdbi = jdbi;
    }


    public Optional<String> findAccount(String tokenHash) {
        return Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("SELECT account_id FROM tokens WHERE token_hash = :token_hash AND revoked IS NULL")
                        .bind("token_hash", tokenHash)
                        .map(StringMapper.FIRST)
                        .first()));
    }

    public List<Map<String,Object>> findTokens(String accountId) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT token_link, description, to_char(revoked,'DD Mon YYYY') as revoked FROM tokens WHERE account_id = :account_id ORDER BY issued DESC")
                        .bind("account_id", accountId)
                        .list());
    }

    public boolean updateTokenDescription(String tokenLink, String newDescription) {
        int rowsUpdated = jdbi.withHandle(handle ->
            handle.update("UPDATE tokens SET description=? WHERE token_link=? AND revoked IS NULL", newDescription, tokenLink)
        );
        return rowsUpdated > 0;
    }

    public void storeToken(String tokenHash, String randomTokenLink, String accountId, String description) {
        Integer rowsUpdated = jdbi.withHandle(handle ->
                        handle.insert("INSERT INTO tokens(token_hash, token_link, description, account_id) VALUES (?,?,?,?)", tokenHash, randomTokenLink, description, accountId)
        );
        if (rowsUpdated != 1) {
            log.error("Unable to store new token for account '{}'. '{}' rows were updated", accountId, rowsUpdated);
            throw new RuntimeException(String.format("Unable to store new token for account %s}", accountId));
        }
    }

    public Optional<String> revokeSingleToken(String accountId, String tokenLink) {
        return Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("UPDATE tokens SET revoked=(now() at time zone 'utc') WHERE account_id=:accountId AND token_link=:tokenLink AND revoked IS NULL RETURNING to_char(revoked,'DD Mon YYYY')")
                        .bind("accountId", accountId)
                        .bind("tokenLink", tokenLink)
                        .map(StringMapper.FIRST)
                        .first()));
    }

}
