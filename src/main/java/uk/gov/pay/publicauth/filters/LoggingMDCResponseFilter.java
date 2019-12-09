package uk.gov.pay.publicauth.filters;

import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

import static uk.gov.pay.logging.LoggingKeys.GATEWAY_ACCOUNT_ID;

public class LoggingMDCResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        MDC.remove(GATEWAY_ACCOUNT_ID);
    }
}
