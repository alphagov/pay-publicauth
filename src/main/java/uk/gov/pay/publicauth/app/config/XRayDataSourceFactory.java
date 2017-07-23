package uk.gov.pay.publicauth.app.config;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.ManagedPooledDataSource;
import io.dropwizard.db.PooledDataSourceFactory;


/**
 * Created by markjones on 20/07/2017.
 */
public class XRayDataSourceFactory extends DataSourceFactory implements PooledDataSourceFactory {

    @Override
    public ManagedDataSource build(MetricRegistry metricRegistry, String s) {
        ManagedDataSource managedPooledDataSource = super.build(metricRegistry, s);
       // ((ManagedPooledDataSource) managedPooledDataSource).setJdbcInterceptors("com.amazonaws.xray.sql.postgres.TracingInterceptor;");
        return managedPooledDataSource;
    }
}
