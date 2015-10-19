package uk.gov.pay.publicauth.dao;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.util.StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

        List<Map<String, Object>> tokens = jdbi.withHandle(handle ->
                handle.createQuery("SELECT token_link, description, to_char(revoked,'DD Mon YYYY') as revoked FROM tokens WHERE account_id = :account_id ORDER BY issued DESC")
                        .bind("account_id", accountId)
                        .list());

        return tokens.stream().map(tokenMap -> {
            if (tokenMap.get("revoked") != null) return tokenMap;
            else {
                Map<String, Object> newMap = new HashMap<>();
                newMap.put("token_link", tokenMap.get("token_link"));
                newMap.put("description", tokenMap.get("description"));
                return newMap;
            }
        }).collect(Collectors.toList());
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
            log.error("Unable to store new token for account {}", accountId);
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

    public boolean revokeMultipleTokens(String accountId) {
        int rowsUpdated = jdbi.withHandle(handle ->
            handle.update("UPDATE tokens SET revoked=(now() at time zone 'utc') WHERE account_id=? AND revoked IS NULL", accountId)
        );
        return rowsUpdated > 0;
    }
}
