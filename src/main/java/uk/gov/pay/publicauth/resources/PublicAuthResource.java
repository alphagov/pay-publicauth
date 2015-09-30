package uk.gov.pay.publicauth.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import uk.gov.pay.publicauth.dao.AuthTokenDao;
import uk.gov.pay.publicauth.service.TokenHasher;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.function.Function;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Path("/")
public class PublicAuthResource {

    public static final String AUTH_PATH = "v1/auth";
    private AuthTokenDao authDao;
    private TokenHasher tokenHasher;

    private final static String BEARER_PREFIX = "Bearer ";

    public PublicAuthResource(AuthTokenDao authDao, TokenHasher tokenHasher) {
        this.authDao = authDao;
        this.tokenHasher = tokenHasher;
    }

    @Path(AUTH_PATH)
    @Produces(APPLICATION_JSON)
    @GET
    public Response authenticate(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearerToken) {
        String tokenId = bearerToken.substring(BEARER_PREFIX.length());
        Optional<String> account = authDao.findAccount(tokenHasher.hash(tokenId));
        return account.map(
                        accountId -> Response.ok(ImmutableMap.of("account_id", accountId)))
                .orElseGet(
                        () -> Response.status(Response.Status.UNAUTHORIZED))
                .build();
    }

    @Path(AUTH_PATH)
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @POST
    public Response revokeToken(JsonNode payload) {
        return withValidAccountId(payload, (accountId) -> {
            String newToken = randomUUID().toString();
            authDao.createToken(tokenHasher.hash(newToken), accountId);
            return Response.ok(ImmutableMap.of("token", newToken)).build();
        });
    }

    @Path(AUTH_PATH + "/{accountId}/revoke")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @POST
    public Response revokeToken(@PathParam("accountId") String accountId) {
        if (authDao.revokeToken(accountId)) {
            return Response.ok().build();
        }
        return Response.status(404).build();
    }


    private Response withValidAccountId(JsonNode payload, Function<String, Response> handler) {
        if (payload == null) {
            return Response.status(BAD_REQUEST).entity(ImmutableMap.of("message","Missing fields: [account_id]")).build();
        }
        JsonNode accountIdNode = payload.get("account_id");
        if (accountIdNode == null) {
            return Response.status(BAD_REQUEST).entity(ImmutableMap.of("message","Missing fields: [account_id]")).build();
        }
        return handler.apply(accountIdNode.asText());
    }
}
