package uk.gov.pay.publicauth.app;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.javax.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.strategy.sampling.LocalizedSamplingStrategy;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.graphite.GraphiteUDP;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedPooledDataSource;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.app.config.PublicAuthConfiguration;
import uk.gov.pay.publicauth.auth.Token;
import uk.gov.pay.publicauth.auth.TokenAuthenticator;
import uk.gov.pay.publicauth.dao.AuthTokenDao;
import uk.gov.pay.publicauth.filters.LoggingFilter;
import uk.gov.pay.publicauth.resources.HealthCheckResource;
import uk.gov.pay.publicauth.resources.PublicAuthResource;
import uk.gov.pay.publicauth.service.TokenService;
import uk.gov.pay.publicauth.util.DependentResourceWaitCommand;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static java.util.EnumSet.of;
import static javax.servlet.DispatcherType.REQUEST;
import static uk.gov.pay.publicauth.resources.PublicAuthResource.API_VERSION_PATH;

public class PublicAuthApp extends Application<PublicAuthConfiguration> {

    private static final String SERVICE_METRICS_NODE = "publicauth";
    private static final int GRAPHITE_SENDING_PERIOD_SECONDS = 10;
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicAuthApp.class);


    private DBI jdbi;

    @Override
    public void initialize(Bootstrap<PublicAuthConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        bootstrap.addBundle(new DBIExceptionsBundle());

        bootstrap.addBundle(new MigrationsBundle<PublicAuthConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(PublicAuthConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });

        bootstrap.addCommand(new DependentResourceWaitCommand());
    }


    @Override
    public void run(PublicAuthConfiguration conf, Environment environment) throws Exception {
        DataSourceFactory dataSourceFactory = conf.getDataSourceFactory();

        jdbi = new DBIFactory().build(environment, dataSourceFactory, "postgresql");
        LOGGER.error(dataSourceFactory.getMaxWaitForConnection().toString());


        initialiseMetrics(conf, environment);

        TokenService tokenService = new TokenService(conf.getTokensConfiguration());

        environment.jersey().register(new AuthDynamicFeature(
                new OAuthCredentialAuthFilter.Builder<Token>()
                        .setAuthenticator(new TokenAuthenticator(tokenService))
                        .setPrefix("Bearer")
                        .buildAuthFilter()));
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(Token.class));

        environment.jersey().register(new PublicAuthResource(new AuthTokenDao(jdbi), tokenService));
        environment.jersey().register(new HealthCheckResource(environment));

        environment.servlets().addFilter("LoggingFilter", new LoggingFilter())
                .addMappingForUrlPatterns(of(REQUEST), true, API_VERSION_PATH + "/*");

        environment.servlets().addFilter("XRayFilter", new AWSXRayServletFilter("pay-publicauth"))
                .addMappingForUrlPatterns(of(REQUEST), true, API_VERSION_PATH + "/*");

    }

    private void initialiseMetrics(PublicAuthConfiguration configuration, Environment environment) {
        GraphiteSender graphiteUDP = new GraphiteUDP(configuration.getGraphiteHost(), Integer.valueOf(configuration.getGraphitePort()));
        GraphiteReporter.forRegistry(environment.metrics())
                .prefixedWith(SERVICE_METRICS_NODE)
                .build(graphiteUDP)
                .start(GRAPHITE_SENDING_PERIOD_SECONDS, TimeUnit.SECONDS);

    }

    public DBI getJdbi() {
        return jdbi;
    }

    public static void main(String[] args) throws Exception {
        AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard();

        URL ruleFile = PublicAuthApp.class.getResource("/sampling-rules.json");
        builder.withSamplingStrategy(new LocalizedSamplingStrategy(ruleFile));

        AWSXRay.setGlobalRecorder(builder.build());
        new PublicAuthApp().run(args);
    }
}
