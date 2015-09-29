package uk.gov.pay.publicauth.utils;

import org.skife.jdbi.v2.DBI;

public class DatabaseTestHelper {
    private DBI jdbi;

    public DatabaseTestHelper(DBI jdbi) {
        this.jdbi = jdbi;
    }

    public void insertAccount(String bearerToken, String accountId) {
        jdbi.withHandle(handle -> handle.insert("INSERT INTO tokens(token_id, account_id) VALUES (?, ?)", bearerToken, accountId));
    }
}
