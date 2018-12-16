package uk.gov.pay.publicauth.dao;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.util.StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.model.TokenHash;
import uk.gov.pay.publicauth.model.TokenLink;
import uk.gov.pay.publicauth.model.TokenPaymentType;
import uk.gov.pay.publicauth.model.TokenState;
import uk.gov.pay.publicauth.model.TokenSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AuthTokenDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenDao.class);

    private final DBI jdbi;

    public AuthTokenDao(DBI jdbi) {
        this.jdbi = jdbi;
    }


    public Optional<Map<String, Object>> findUnRevokedAccount(TokenHash tokenHash) {
        Optional<Map<String, Object>> storedTokenHash = Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("SELECT account_id, type, coalesce(token_type, 'CARD') as token_type FROM tokens WHERE token_hash = :token_hash AND revoked IS NULL")
                        .bind("token_hash", tokenHash.getValue())
                        .first()));
        if (storedTokenHash.isPresent()) {
            updateLastUsedTime(tokenHash);
        }
        return storedTokenHash;
    }

    private void updateLastUsedTime(TokenHash tokenHash) {
        jdbi.withHandle(handle ->
                handle.update("UPDATE tokens SET last_used=(now() at time zone 'utc') WHERE token_hash=?", tokenHash.getValue())
        );
    }

    public List<Map<String, Object>> findTokensBy(String accountId, TokenState tokenState, TokenSource tokenSource) {
        String revoked = (tokenState.equals(TokenState.REVOKED)) ? "AND revoked IS NOT NULL " : "AND revoked IS NULL ";
        String revokedDate = (tokenState.equals(TokenState.REVOKED)) ? "to_char(revoked,'DD Mon YYYY - HH24:MI') as revoked, " : "";

        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT token_link, description, coalesce(token_type, 'CARD') as token_type, type, " +
                        "to_char(issued,'DD Mon YYYY - HH24:MI') as issued_date, " +
                        revokedDate +
                        "created_by, " +
                        "to_char(last_used,'DD Mon YYYY - HH24:MI') as last_used " +
                        "FROM tokens WHERE account_id = :account_id " +
                        "AND type = :type " +
                        revoked +
                        "ORDER BY issued DESC")
                        .bind("account_id", accountId)
                        .bind("type", tokenSource)
                        .list());
    }

    public boolean updateTokenDescription(TokenLink tokenLink, String newDescription) {
        int rowsUpdated = jdbi.withHandle(handle ->
                handle.update("UPDATE tokens SET description=? WHERE token_link=? AND revoked IS NULL", newDescription, tokenLink.getValue())
        );
        return rowsUpdated > 0;
    }

    public void storeToken(TokenHash tokenHash, TokenLink randomTokenLink, TokenSource tokenSource, String accountId, String description, String createdBy, TokenPaymentType tokenPaymentType) {
        Integer rowsUpdated = jdbi.withHandle(handle ->
                handle.insert("INSERT INTO tokens(token_hash, token_link, type, description, account_id, created_by, token_type) VALUES (?,?,?,?,?,?,?)",
                        tokenHash.getValue(), randomTokenLink.getValue(), tokenSource, description, accountId, createdBy, tokenPaymentType)
        );
        if (rowsUpdated != 1) {
            LOGGER.error("Unable to store new token for account '{}'. '{}' rows were updated", accountId, rowsUpdated);
            throw new RuntimeException(String.format("Unable to store new token for account %s}", accountId));
        }
    }
    
    public Optional<String> revokeSingleToken(String accountId, TokenHash tokenHash) {
        return Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("UPDATE tokens SET revoked=(now() at time zone 'utc') WHERE account_id=:accountId AND token_hash=:tokenHash AND revoked IS NULL RETURNING to_char(revoked,'DD Mon YYYY')")
                        .bind("accountId", accountId)
                        .bind("tokenHash", tokenHash.getValue())
                        .map(StringMapper.FIRST)
                        .first()));
    }
    public Optional<String> revokeSingleToken(String accountId, TokenLink tokenLink) {
        return Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("UPDATE tokens SET revoked=(now() at time zone 'utc') WHERE account_id=:accountId AND token_link=:tokenLink AND revoked IS NULL RETURNING to_char(revoked,'DD Mon YYYY')")
                        .bind("accountId", accountId)
                        .bind("tokenLink", tokenLink.getValue())
                        .map(StringMapper.FIRST)
                        .first()));
    }
    public Optional<Map<String, Object>> findTokenByTokenLink(TokenLink tokenLink) {
        return Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("SELECT token_link, type, description, coalesce(token_type, 'CARD') as token_type, " +
                        "to_char(revoked,'DD Mon YYYY - HH24:MI') as revoked, " +
                        "to_char(issued,'DD Mon YYYY - HH24:MI') as issued_date, " +
                        "created_by, " +
                        "to_char(last_used,'DD Mon YYYY - HH24:MI') as last_used " +
                        "FROM tokens WHERE token_link = :token_link")
                        .bind("token_link", tokenLink.getValue())
                        .first()));
    }
}
