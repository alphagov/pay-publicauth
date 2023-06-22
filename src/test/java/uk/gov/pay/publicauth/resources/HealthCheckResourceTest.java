package uk.gov.pay.publicauth.resources;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.jayway.jsonassert.JsonAssert;
import io.dropwizard.core.setup.Environment;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.core.Response;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthCheckResourceTest {

    @Mock
    private Environment environment;

    @Mock
    private HealthCheckRegistry healthCheckRegistry;

    private HealthCheckResource resource;

    @BeforeEach
    void setup() {
        when(environment.healthChecks()).thenReturn(healthCheckRegistry);
        resource = new HealthCheckResource(environment);
    }

    @Test
    void checkHealthCheck_isUnHealthy() throws JsonProcessingException {
        SortedMap<String,HealthCheck.Result> map = new TreeMap<>();
        map.put("postgresql", HealthCheck.Result.unhealthy("database is down"));
        map.put("deadlocks", HealthCheck.Result.unhealthy("no new threads available"));
        when(healthCheckRegistry.runHealthChecks()).thenReturn(map);
        Response response = resource.healthCheck();
        assertThat(response.getStatus(), is(503));
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String body = ow.writeValueAsString(response.getEntity());

        JsonAssert.with(body)
            .assertThat("$.*", hasSize(2))
            .assertThat("$.postgresql.healthy", Is.is(false))
            .assertThat("$.deadlocks.healthy", Is.is(false));
    }

    @Test
    void checkHealthCheck_isHealthy() throws JsonProcessingException {
        SortedMap<String,HealthCheck.Result> map = new TreeMap<>();
        map.put("postgresql", HealthCheck.Result.healthy());
        map.put("deadlocks", HealthCheck.Result.healthy());
        when(healthCheckRegistry.runHealthChecks()).thenReturn(map);
        Response response = resource.healthCheck();
        assertThat(response.getStatus(), is(200));
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String body = ow.writeValueAsString(response.getEntity());

        JsonAssert.with(body)
            .assertThat("$.*", hasSize(2))
            .assertThat("$.postgresql.healthy", Is.is(true))
            .assertThat("$.deadlocks.healthy", Is.is(true));
    }
}
