package uk.gov.pay.publicauth.exception;

import uk.gov.service.payments.commons.model.ErrorIdentifier;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.util.Map;

public class TokenInvalidExceptionMapper implements ExceptionMapper<TokenInvalidException> {
    @Override
    public Response toResponse(TokenInvalidException e) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of(
                        "message", e.getMessage(),
                        "error_identifier", ErrorIdentifier.AUTH_TOKEN_INVALID
                ))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
