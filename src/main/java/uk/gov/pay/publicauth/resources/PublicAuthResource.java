package uk.gov.pay.publicauth.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.dao.AuthTokenDao;
import uk.gov.pay.publicauth.service.TokenHasher;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Path("/")
public class PublicAuthResource {

    private final Logger logger = LoggerFactory.getLogger(PublicAuthResource.class);

    public static final String API_AUTH_PATH = "/v1/api/auth";
    private static final String FRONTEND_AUTH_PATH = "/v1/frontend/auth";

    public static final Response.ResponseBuilder UNAUTHORISED = Response.status(Response.Status.UNAUTHORIZED);
    private AuthTokenDao authDao;
    private TokenHasher tokenHasher;

    private final static String BEARER_PREFIX = "Bearer ";

    public PublicAuthResource(AuthTokenDao authDao, TokenHasher tokenHasher) {
        this.authDao = authDao;
        this.tokenHasher = tokenHasher;
    }

    @Path(API_AUTH_PATH)
    @Produces(APPLICATION_JSON)
    @GET
    public Response authenticate(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearerToken) {
        if (bearerToken == null) {
            return UNAUTHORISED.build();
        }
        String tokenId = bearerToken.substring(BEARER_PREFIX.length());
        Optional<String> account = authDao.findAccount(tokenHasher.hash(tokenId));
        return account.map(
                        accountId -> Response.ok(ImmutableMap.of("account_id", accountId)))
                .orElseGet(
                        () -> UNAUTHORISED)
                .build();
    }

    @Path(FRONTEND_AUTH_PATH)
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @POST
    public Response createTokenForAccount(JsonNode payload) {
        return withValidAccountIdAndDescription(payload, (accountId, description) -> {
            String newToken = randomUUID().toString();
            String randomTokenLink = randomUUID().toString();
            authDao.storeToken(tokenHasher.hash(newToken), randomTokenLink, accountId, description);
            return Response.ok(ImmutableMap.of("token", newToken)).build();
        });
    }

    @Path(FRONTEND_AUTH_PATH + "/{accountId}")
    @Produces(APPLICATION_JSON)
    @GET
    public Response getIssuedTokensForAccount(@PathParam("accountId") String accountId) {
        List<Map<String, Object>> tokens = authDao.findTokens(accountId);
        return Response.ok(ImmutableMap.of("tokens", tokens)).build();
    }

    @Path(FRONTEND_AUTH_PATH + "/{accountId}/revoke")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @POST
    public Response revokeToken(@PathParam("accountId") String accountId) {
        if (authDao.revokeToken(accountId)) {
            return Response.ok().build();
        }
        return Response.status(404).build();
    }


    private Response withValidAccountIdAndDescription(JsonNode payload, BiFunction<String, String, Response> handler) {
        if (payload == null) {
            return Response.status(BAD_REQUEST).entity(ImmutableMap.of("message","Missing fields: [account_id, description]")).build();
        }

        Queue<String> missingFields = new LinkedList<>();
        JsonNode accountIdNode = payload.get("account_id");
        if (accountIdNode == null) missingFields.add("account_id");
        JsonNode descriptionNode = payload.get("description");
        if (descriptionNode == null) missingFields.add("description");

        return missingFields.size()>0 ?
                Response.status(BAD_REQUEST).entity(ImmutableMap.of("message", "Missing fields: [" + missingFields.stream().collect(Collectors.joining(", "))+"]")).build() :
                handler.apply(accountIdNode.asText(), descriptionNode.asText());
    }
}
