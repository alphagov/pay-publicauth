package uk.gov.pay.publicauth.dao;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.util.StringMapper;

import java.util.Optional;

public class AuthTokenDao {
    private DBI jdbi;

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
}
