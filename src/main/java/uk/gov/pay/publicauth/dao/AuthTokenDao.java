package uk.gov.pay.publicauth.dao;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.util.StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.util.UUID.randomUUID;

public class AuthTokenDao {
    private DBI jdbi;
    public static Logger log = LoggerFactory.getLogger(AuthTokenDao.class);

    public AuthTokenDao(DBI jdbi) {
        this.jdbi = jdbi;
    }


    public Optional<String> findAccount(String bearerToken) {
        return Optional.ofNullable(jdbi.withHandle(handle ->
                handle.createQuery("SELECT account_id FROM tokens WHERE token_id = :token_id and revoked_date IS NULL")
                        .bind("token_id", bearerToken)
                        .map(StringMapper.FIRST)
                        .first()));
    }

    public void createToken(String token, String accountId) {
        Integer insertStatus = jdbi.withHandle(handle ->
                        handle.insert("INSERT INTO tokens(token_id, account_id) VALUES (?,?)", token, accountId)
        );
        if (insertStatus != 1) {
            log.error("Unable to store newToken for account {}", accountId);
            throw new RuntimeException("Server error when issuing a new token");
        }
    }

}
