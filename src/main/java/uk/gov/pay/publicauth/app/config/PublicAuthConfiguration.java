package uk.gov.pay.publicauth.app.config;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class PublicAuthConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("database")
    private DataSourceFactory dataSourceFactory = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("tokensConfig")
    private TokensConfiguration TokensConfiguration;

    public DataSourceFactory getDataSourceFactory() {
        return dataSourceFactory;
    }

    public TokensConfiguration getTokensConfiguration() {
        return TokensConfiguration;
    }
}
