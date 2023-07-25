package uk.gov.pay.publicauth.app.config;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Optional;

public class PublicAuthConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("database")
    private final DataSourceFactory dataSourceFactory = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty("tokensConfig")
    private TokensConfiguration TokensConfiguration;

    @NotNull
    private String graphiteHost;

    @NotNull
    private Integer graphitePort;

    @JsonProperty("ecsContainerMetadataUriV4")
    private URI ecsContainerMetadataUriV4;

    public DataSourceFactory getDataSourceFactory() {
        return dataSourceFactory;
    }

    public TokensConfiguration getTokensConfiguration() {
        return TokensConfiguration;
    }

    public String getGraphiteHost() {
        return graphiteHost;
    }

    public Integer getGraphitePort() {
        return graphitePort;
    }

    public Optional<URI> getEcsContainerMetadataUriV4() {
        return Optional.ofNullable(ecsContainerMetadataUriV4);
    }
}
