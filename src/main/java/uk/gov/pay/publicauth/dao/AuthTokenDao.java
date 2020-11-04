package uk.gov.pay.publicauth.dao;

import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.model.CreateTokenRequest;
import uk.gov.pay.publicauth.model.TokenHash;
import uk.gov.pay.publicauth.model.TokenLink;
import uk.gov.pay.publicauth.model.TokenSource;
import uk.gov.pay.publicauth.model.TokenState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AuthTokenDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenDao.class);

    private final Jdbi jdbi;

    public AuthTokenDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Optional<Map<String, Object>> findUnRevokedAccount(TokenHash tokenHash) {
        Optional<Map<String, Object>> storedTokenHash = jdbi.withHandle(handle ->
                handle.createQuery("SELECT account_id, type, coalesce(token_type, 'CARD') as token_type FROM tokens WHERE token_hash = :token_hash AND revoked IS NULL")
                        .bind("token_hash", tokenHash.getValue())
                        .mapToMap().findFirst());
        if (storedTokenHash.isPresent()) {
            updateLastUsedTime(tokenHash);
        }
        return storedTokenHash;
    }

    private void updateLastUsedTime(TokenHash tokenHash) {
        jdbi.withHandle(handle ->
                handle.createUpdate("UPDATE tokens SET last_used=(now() at time zone 'utc') WHERE token_hash=:token_hash")
                        .bind("token_hash", tokenHash.getValue())
                        .execute());
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
                        .mapToMap()
                        .list());
    }

    public boolean updateTokenDescription(TokenLink tokenLink, String newDescription) {
        int rowsUpdated = jdbi.withHandle(handle ->
                handle.createUpdate("UPDATE tokens SET description=:description WHERE token_link=:token_link AND revoked IS NULL")
                        .bind("description", newDescription)
                        .bind("token_link", tokenLink.toString()).execute());
        return rowsUpdated > 0;
    }

    public void storeToken(TokenHash tokenHash, CreateTokenRequest createTokenRequest) {
        Integer rowsUpdated = jdbi.withHandle(handle ->
                handle.createUpdate("INSERT INTO tokens(token_hash, token_link, type, description, account_id, created_by, token_type) " +
                        "VALUES (:token_hash,:token_link,:type,:description,:account_id,:created_by,:token_type)")
                        .bind("token_hash", tokenHash.getValue())
                        .bind("token_link", createTokenRequest.getTokenLink().toString())
                        .bind("type", createTokenRequest.getTokenSource())
                        .bind("description", createTokenRequest.getDescription())
                        .bind("account_id", createTokenRequest.getAccountId())
                        .bind("created_by", createTokenRequest.getCreatedBy())
                        .bind("token_type", createTokenRequest.getTokenPaymentType())
                        .execute());
        if (rowsUpdated != 1) {
            LOGGER.error("Unable to store new token for account '{}'. '{}' rows were updated", createTokenRequest.getAccountId(), rowsUpdated);
            throw new RuntimeException(String.format("Unable to store new token for account %s}", createTokenRequest.getAccountId()));
        }
    }

    public Optional<String> revokeSingleToken(String accountId, TokenHash tokenHash) {
        return Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("UPDATE tokens SET revoked=(now() at time zone 'utc') WHERE account_id=:account_id AND token_hash=:token_hash AND revoked IS NULL RETURNING to_char(revoked,'DD Mon YYYY')")
                        .bind("account_id", accountId)
                        .bind("token_hash", tokenHash.getValue())
                        .mapTo(String.class)
                        .first()));
    }

    public Optional<String> revokeSingleToken(String accountId, TokenLink tokenLink) {
        return jdbi.withHandle(handle ->
                handle.createQuery("UPDATE tokens SET revoked=(now() at time zone 'utc') WHERE account_id=:account_id AND token_link=:token_link AND revoked IS NULL RETURNING to_char(revoked,'DD Mon YYYY')")
                        .bind("account_id", accountId)
                        .bind("token_link", tokenLink.toString())
                        .mapTo(String.class)
                        .findFirst());
    }

    public Optional<Map<String, Object>> findTokenByTokenLink(TokenLink tokenLink) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT token_link, type, description, coalesce(token_type, 'CARD') as token_type, " +
                        "to_char(revoked,'DD Mon YYYY - HH24:MI') as revoked, " +
                        "to_char(issued,'DD Mon YYYY - HH24:MI') as issued_date, " +
                        "created_by, " +
                        "to_char(last_used,'DD Mon YYYY - HH24:MI') as last_used " +
                        "FROM tokens WHERE token_link = :token_link")
                        .bind("token_link", tokenLink.toString())
                        .mapToMap().findFirst());
    }
}
