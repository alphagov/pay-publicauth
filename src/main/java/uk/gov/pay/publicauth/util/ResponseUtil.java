package uk.gov.pay.publicauth.util;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

public class ResponseUtil {

    public static Response notFoundResponse(Logger logger, String message) {
        logger.error(message);
        return responseWithMessage(NOT_FOUND, message);
    }

    public static Response badRequestResponse(Logger logger, String message) {
        logger.error(message);
        return responseWithMessage(BAD_REQUEST, message);
    }

    public static Response serverErrorResponse(Logger logger, String message) {
        logger.error(message);
        return responseWithMessage(INTERNAL_SERVER_ERROR, message);
    }

    private static Response responseWithMessage(Response.Status status, String message) {
        return Response.status(status).entity(ImmutableMap.of("message", message)).build();
    }

}
