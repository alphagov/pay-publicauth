package uk.gov.pay.publicauth.app;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.skife.jdbi.v2.DBI;
import uk.gov.pay.publicauth.dao.AuthTokenDao;
import uk.gov.pay.publicauth.resources.PublicAuthResource;
import uk.gov.pay.publicauth.service.TokenHasher;
import uk.gov.pay.publicauth.util.DependentResourceWaitCommand;

public class PublicAuthApp extends Application<PublicAuthConfiguration> {
    private DBI jdbi;

    @Override
    public void initialize(Bootstrap<PublicAuthConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor()
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
        environment.jersey().register(new PublicAuthResource(new AuthTokenDao(jdbi), new TokenHasher()));
    }

    public DBI getJdbi() {
        return jdbi;
    }

    public static void main(String[] args) throws Exception {
        new PublicAuthApp().run(args);
    }
}
