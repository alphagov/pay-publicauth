package uk.gov.pay.publicauth.exception;

import uk.gov.service.payments.commons.model.ErrorIdentifier;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Map;

public class TokenRevokedExceptionMapper implements ExceptionMapper<TokenRevokedException> {
    @Override
    public Response toResponse(TokenRevokedException e) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of(
                        "message", e.getMessage(),
                        "token_link", e.getTokenLink().toString(),
                        "error_identifier", ErrorIdentifier.AUTH_TOKEN_REVOKED
                ))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
