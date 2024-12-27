package uk.gov.pay.publicauth.exception;

import com.google.common.collect.ImmutableMap;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        return Response.status(BAD_REQUEST)
                .entity(ImmutableMap.of("message", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

}
