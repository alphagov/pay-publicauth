package uk.gov.pay.publicauth.exception;

import com.google.common.collect.ImmutableMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        return Response.status(BAD_REQUEST)
                .entity(ImmutableMap.of("message", exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

}
