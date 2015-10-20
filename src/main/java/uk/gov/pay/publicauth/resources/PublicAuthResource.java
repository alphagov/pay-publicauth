package uk.gov.pay.publicauth.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.dao.AuthTokenDao;
import uk.gov.pay.publicauth.service.TokenHasher;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ok;
import static uk.gov.pay.publicauth.util.ResponseUtil.badRequestResponse;
import static uk.gov.pay.publicauth.util.ResponseUtil.notFoundResponse;

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
        return authDao.findAccount(tokenHasher.hash(tokenId))
                .map(accountId -> ok(ImmutableMap.of("account_id", accountId)))
                .orElseGet(() -> UNAUTHORISED)
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
            return ok(ImmutableMap.of("token", newToken)).build();
        });
    }

    @Path(FRONTEND_AUTH_PATH + "/{accountId}")
    @Produces(APPLICATION_JSON)
    @GET
    public Response getIssuedTokensForAccount(@PathParam("accountId") String accountId) {
        List<Map<String, Object>> tokensWithoutNullRevoked = authDao.findTokens(accountId)
                .stream()
                .map(tokenMap -> {
                    if (tokenMap.get("revoked") == null) tokenMap.remove("revoked");
                    return tokenMap;
                })
                .collect(Collectors.toList());

        return ok(ImmutableMap.of("tokens", tokensWithoutNullRevoked)).build();
    }

    @Path(FRONTEND_AUTH_PATH)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @PUT
    public Response updateTokenDescription(JsonNode payload) {
        return withTokenLinkAndDescription(payload, (token_link, description) -> {
            boolean updated = authDao.updateTokenDescription(token_link, description);
            if (updated) {
                return ok(ImmutableMap.of("token_link", token_link, "description", description)).build();
            }
            return notFoundResponse(logger, "Could not update token description");
        });
    }

    @Path(FRONTEND_AUTH_PATH + "/{accountId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @DELETE
    public Response revokeSingleToken(@PathParam("accountId") String accountId, JsonNode payload) {
        JsonNode jsonNode;
        if (payload == null ||  (jsonNode = payload.get("token_link")) == null) {
            return badRequestResponse(logger, "Missing fields: [token_link]");
        }

        return authDao.revokeSingleToken(accountId, jsonNode.asText())
                .map(revokedDate -> ok(ImmutableMap.of("revoked", revokedDate)).build())
                .orElseGet(() -> notFoundResponse(logger, "Could not revoke token"));
    }

    private Response withTokenLinkAndDescription(JsonNode requestPayload, BiFunction<String, String, Response> handler) {
        if (requestPayload == null) {
            return badRequestResponse(logger, "Missing fields: [token_link, description]");
        }

        List<String> missingFieldsInRequestPayload = findMissingFieldsInRequestPayload(requestPayload, "token_link", "description");

        if (missingFieldsInRequestPayload.isEmpty()) {
            return handler.apply(requestPayload.get("token_link").asText(), requestPayload.get("description").asText());
        }
        return badRequestResponse(logger, "Missing fields: [" + Joiner.on(", ").join(missingFieldsInRequestPayload) + "]");
    }

    private Response withValidAccountIdAndDescription(JsonNode requestPayload, BiFunction<String, String, Response> handler) {
        if (requestPayload == null) {
            return badRequestResponse(logger, "Missing fields: [account_id, description]");
        }

        List<String> missingFieldsInRequestPayload = findMissingFieldsInRequestPayload(requestPayload, "account_id", "description");

        if (missingFieldsInRequestPayload.isEmpty()) {
            return handler.apply(requestPayload.get("account_id").asText(), requestPayload.get("description").asText());
        }
        return badRequestResponse(logger, "Missing fields: [" + Joiner.on(", ").join(missingFieldsInRequestPayload) + "]");
    }

    private List<String> findMissingFieldsInRequestPayload(JsonNode requestPayload, String... expectedFieldsInRequestPayload) {
        return Stream.of(expectedFieldsInRequestPayload)
                .filter(expectedKey -> requestPayload.get(expectedKey) == null)
                .collect(Collectors.toList());
    }
}
