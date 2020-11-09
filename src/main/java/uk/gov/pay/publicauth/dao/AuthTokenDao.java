package uk.gov.pay.publicauth.dao;

import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.model.CreateTokenRequest;
import uk.gov.pay.publicauth.model.TokenEntity;
import uk.gov.pay.publicauth.model.TokenHash;
import uk.gov.pay.publicauth.model.TokenLink;
import uk.gov.pay.publicauth.model.TokenSource;
import uk.gov.pay.publicauth.model.TokenState;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public class AuthTokenDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenDao.class);

    private static final String TOKEN_SELECT =
            "SELECT token_link, description, account_id, token_type, type, issued, revoked, last_used, created_by FROM tokens ";

    private final Jdbi jdbi;

    public AuthTokenDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Optional<TokenEntity> findTokenByHash(TokenHash tokenHash) {
        return jdbi.withHandle(handle ->
                    handle.createQuery(TOKEN_SELECT +
                            "WHERE token_hash = :token_hash")
                            .bind("token_hash", tokenHash.getValue())
                            .map(new TokenMapper())
                            .findFirst());
    }

    public void updateLastUsedTime(TokenHash tokenHash) {
        jdbi.withHandle(handle ->
                handle.createUpdate("UPDATE tokens SET last_used=(now() at time zone 'utc') WHERE token_hash=:token_hash")
                        .bind("token_hash", tokenHash.getValue())
                        .execute());
    }

    public List<TokenEntity> findTokensBy(String accountId, TokenState tokenState, TokenSource tokenSource) {
        String revokedClause = (tokenState.equals(TokenState.REVOKED)) ? "AND revoked IS NOT NULL " : "AND revoked IS NULL ";

        return jdbi.withHandle(handle ->
                handle.createQuery(TOKEN_SELECT +
                        "WHERE account_id = :account_id " +
                        "AND type = :type " +
                        revokedClause +
                        "ORDER BY issued DESC")
                        .bind("account_id", accountId)
                        .bind("type", tokenSource)
                        .map(new TokenMapper())
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

    public Optional<ZonedDateTime> revokeSingleToken(String accountId, TokenHash tokenHash) {
        return Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("UPDATE tokens SET revoked=(now() at time zone 'utc') WHERE account_id=:account_id AND token_hash=:token_hash AND revoked IS NULL RETURNING revoked")
                        .bind("account_id", accountId)
                        .bind("token_hash", tokenHash.getValue())
                        .mapTo(ZonedDateTime.class)
                        .first()));
    }

    public Optional<ZonedDateTime> revokeSingleToken(String accountId, TokenLink tokenLink) {
        return jdbi.withHandle(handle ->
                handle.createQuery("UPDATE tokens SET revoked=(now() at time zone 'utc') WHERE account_id=:account_id AND token_link=:token_link AND revoked IS NULL RETURNING revoked")
                        .bind("account_id", accountId)
                        .bind("token_link", tokenLink.toString())
                        .mapTo(ZonedDateTime.class)
                        .findFirst());
    }

    public Optional<TokenEntity> findTokenByTokenLink(TokenLink tokenLink) {
        return jdbi.withHandle(handle ->
                handle.createQuery(TOKEN_SELECT + "WHERE token_link = :token_link")
                        .bind("token_link", tokenLink.toString())
                        .map(new TokenMapper())
                        .findFirst());
    }
}
