package uk.gov.pay.publicauth.model;

public class Tokens {

    private final String hashedToken;

    private final String apiKey;

    public Tokens(String hashedToken, String apiKey) {
        this.hashedToken = hashedToken;
        this.apiKey = apiKey;
    }

    public String getHashedToken() {
        return hashedToken;
    }

    public String getApiKey() {
        return apiKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tokens tokens = (Tokens) o;

        if (!hashedToken.equals(tokens.hashedToken)) return false;
        return apiKey.equals(tokens.apiKey);

    }

    @Override
    public int hashCode() {
        int result = hashedToken.hashCode();
        result = 31 * result + apiKey.hashCode();
        return result;
    }
}
