package uk.gov.pay.publicauth.util;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import uk.gov.pay.publicauth.app.config.PublicAuthConfiguration;

public class DependentResourceWaitCommand extends ConfiguredCommand<PublicAuthConfiguration> {

    public DependentResourceWaitCommand() {
        super("waitOnDependencies", "Waits for dependent resources to become available");
    }

    @Override
    protected void run(Bootstrap<PublicAuthConfiguration> bs, Namespace ns, PublicAuthConfiguration conf) {
        ApplicationStartupDependentResourceChecker applicationStartupDependentResourceChecker = new ApplicationStartupDependentResourceChecker(new ApplicationStartupDependentResource(conf));
        applicationStartupDependentResourceChecker.checkAndWaitForResources();
    }
}
