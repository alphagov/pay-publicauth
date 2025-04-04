package uk.gov.pay.publicauth.exception;

import com.google.common.collect.ImmutableMap;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

public class TokenNotFoundExceptionMapper implements ExceptionMapper<TokenNotFoundException> {

    @Override
    public Response toResponse(TokenNotFoundException exception) {
        return Response.status(NOT_FOUND)
                .entity(ImmutableMap.of("message", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
