package uk.gov.pay.publicauth.app.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class TokensConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("encryptDBSalt")
    private String encryptDBSalt;

    @Valid
    @NotNull
    @JsonProperty("apiKeyHmacSecret")
    private String apiKeyHmacSecret;

    public String getEncryptDBSalt() {
        return encryptDBSalt;
    }

    public String getApiKeyHmacSecret() {
        return apiKeyHmacSecret;
    }
}
