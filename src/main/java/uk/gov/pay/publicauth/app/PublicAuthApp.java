package uk.gov.pay.publicauth.app;

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
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.skife.jdbi.v2.DBI;
import uk.gov.pay.commons.utils.healthchecks.DatabaseHealthCheck;
import uk.gov.pay.commons.utils.logging.LoggingFilter;
import uk.gov.pay.commons.utils.metrics.DatabaseMetricsService;
import uk.gov.pay.publicauth.app.config.PublicAuthConfiguration;
import uk.gov.pay.publicauth.auth.Token;
import uk.gov.pay.publicauth.auth.TokenAuthenticator;
import uk.gov.pay.publicauth.dao.AuthTokenDao;
import uk.gov.pay.publicauth.exception.TokenNotFoundExceptionMapper;
import uk.gov.pay.publicauth.exception.ValidationExceptionMapper;
import uk.gov.pay.publicauth.resources.HealthCheckResource;
import uk.gov.pay.publicauth.resources.PublicAuthResource;
import uk.gov.pay.publicauth.service.TokenService;
import uk.gov.pay.publicauth.util.DependentResourceWaitCommand;

import java.util.concurrent.TimeUnit;

import static java.util.EnumSet.of;
import static javax.servlet.DispatcherType.REQUEST;

public class PublicAuthApp extends Application<PublicAuthConfiguration> {

    private static final String SERVICE_METRICS_NODE = "publicauth";
    private static final int GRAPHITE_SENDING_PERIOD_SECONDS = 10;

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
    public void run(PublicAuthConfiguration conf, Environment environment) {
        DataSourceFactory dataSourceFactory = conf.getDataSourceFactory();

        jdbi = new DBIFactory().build(environment, dataSourceFactory, "postgresql");
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
        environment.jersey().register(new ValidationExceptionMapper());
        environment.jersey().register(new TokenNotFoundExceptionMapper());
        environment.healthChecks().register("database", new DatabaseHealthCheck(conf.getDataSourceFactory()));

        environment.servlets().addFilter("LoggingFilter", new LoggingFilter())
                .addMappingForUrlPatterns(of(REQUEST), true, "/v1" + "/*");
    }

    private void initialiseMetrics(PublicAuthConfiguration configuration, Environment environment) {
        DatabaseMetricsService metricsService = new DatabaseMetricsService(configuration.getDataSourceFactory(), environment.metrics(), "publicauth");

        environment
                .lifecycle()
                .scheduledExecutorService("metricscollector")
                .threads(1)
                .build()
                .scheduleAtFixedRate(metricsService::updateMetricData, 0, GRAPHITE_SENDING_PERIOD_SECONDS / 2, TimeUnit.SECONDS);

        GraphiteSender graphiteUDP = new GraphiteUDP(configuration.getGraphiteHost(), configuration.getGraphitePort());
        GraphiteReporter.forRegistry(environment.metrics())
                .prefixedWith(SERVICE_METRICS_NODE)
                .build(graphiteUDP)
                .start(GRAPHITE_SENDING_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    public DBI getJdbi() {
        return jdbi;
    }

    public static void main(String[] args) throws Exception {
        new PublicAuthApp().run(args);
    }
}
