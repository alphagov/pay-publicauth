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
        new ApplicationStartupDependentResourceChecker(new DatabaseStartupResource(conf), duration -> {
            try {
                Thread.sleep(duration.getNano() / 1000);
            } catch (InterruptedException ignored) {
            }
        })
                .checkAndWaitForResources();
    }
}
