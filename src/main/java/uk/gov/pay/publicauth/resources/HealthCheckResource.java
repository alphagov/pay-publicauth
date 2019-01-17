package uk.gov.pay.publicauth.resources;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.setup.Environment;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/")
public class HealthCheckResource {

    private final Environment environment;

    public HealthCheckResource(Environment environment) {
        this.environment = environment;
    }

    @GET
    @Path("healthcheck")
    @Produces(APPLICATION_JSON)
    public Response healthCheck() {
        SortedMap<String, HealthCheck.Result> results = environment.healthChecks().runHealthChecks();

        Map<String, Map<String, Boolean>> response = results.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        healthCheck -> ImmutableMap.of("healthy", healthCheck.getValue().isHealthy())));

        boolean allHealthy = results.values().stream().allMatch(HealthCheck.Result::isHealthy);

        Response.ResponseBuilder res = allHealthy ? Response.ok() : Response.status(503);

        return res.entity(response).build();
    }
}
