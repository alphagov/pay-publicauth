package uk.gov.pay.publicauth.service;

import org.mindrot.jbcrypt.BCrypt;

public class TokenHasher {

    public String hash(String bearerToken) {
        // Hard-coded salt (for now?) because it cannot be unique per token, as we don't
        // have other information to tell us which hashed password to compare candidates with.

        return BCrypt.hashpw(bearerToken, "$2a$10$IhaXo6LIBhKIWOiGpbtPOu");
    }

}
