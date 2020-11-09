package uk.gov.pay.publicauth.exception;

public class TokenInvalidException extends RuntimeException {
    
    public TokenInvalidException(String message) {
        super(message);
    }
}
