package uk.gov.pay.publicauth.util;


import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import uk.gov.service.payments.commons.utils.startup.ApplicationStartupDependentResourceChecker;
import uk.gov.service.payments.commons.utils.startup.DatabaseStartupResource;
import uk.gov.pay.publicauth.app.config.PublicAuthConfiguration;

public class DependentResourceWaitCommand extends ConfiguredCommand<PublicAuthConfiguration> {

    public DependentResourceWaitCommand() {
        super("waitOnDependencies", "Waits for dependent resources to become available");
    }

    @Override
    protected void run(Bootstrap<PublicAuthConfiguration> bs, Namespace ns, PublicAuthConfiguration conf) {
        new ApplicationStartupDependentResourceChecker(new DatabaseStartupResource(conf.getDataSourceFactory()))
                .checkAndWaitForResource();
    }
}
