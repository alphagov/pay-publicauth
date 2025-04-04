package uk.gov.pay.publicauth.filters;

import org.slf4j.MDC;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.util.Optional;

import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_ACCOUNT_ID;

public class LoggingMDCRequestFilter implements ContainerRequestFilter {


    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        getPathParameterFromRequest("accountId", requestContext)
                .ifPresent(accountId -> MDC.put(GATEWAY_ACCOUNT_ID, accountId));
    }

    private Optional<String> getPathParameterFromRequest(String parameterName, ContainerRequestContext requestContext) {
        return Optional.ofNullable(requestContext.getUriInfo().getPathParameters().getFirst(parameterName));
    }
}
