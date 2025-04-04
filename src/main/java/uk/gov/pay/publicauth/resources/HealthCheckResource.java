package uk.gov.pay.publicauth.resources;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.core.setup.Environment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/")
public class HealthCheckResource {

    private final Environment environment;

    public HealthCheckResource(Environment environment) {
        this.environment = environment;
    }

    @GET
    @Path("healthcheck")
    @Produces(APPLICATION_JSON)
    @Operation(
            tags = "Other",
            summary = "Healthcheck endpoint for webhooks. Check database, and deadlocks",
            responses = {
                    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(example = "{" +
                            "    \"postgres\": {" +
                            "        \"healthy\": true," +
                            "        \"message\": \"Healthy\"" +
                            "    }," +
                            "    \"deadlocks\": {" +
                            "        \"healthy\": true," +
                            "        \"message\": \"Healthy\"" +
                            "    }" +
                            "}")), description = "OK"),
                    @ApiResponse(responseCode = "503", description = "Service unavailable. If any healthchecks fail")
            }
    )
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
