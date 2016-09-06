package uk.gov.pay.publicauth.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.dao.AuthTokenDao;
import uk.gov.pay.publicauth.model.Tokens;
import uk.gov.pay.publicauth.service.TokenService;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.*;
import static uk.gov.pay.publicauth.util.ResponseUtil.badRequestResponse;
import static uk.gov.pay.publicauth.util.ResponseUtil.notFoundResponse;

@Singleton
@Path("/")
public class PublicAuthResource {

    public static final String API_VERSION_PATH = "/v1";

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicAuthResource.class);

    private static final ResponseBuilder UNAUTHORISED = status(Status.UNAUTHORIZED);
    private static final String API_AUTH_PATH = API_VERSION_PATH + "/api/auth";
    private static final String FRONTEND_AUTH_PATH = API_VERSION_PATH + "/frontend/auth";
    private static final String FRONTEND_AUTH_PATH_NEW = API_VERSION_PATH + "/frontend/authorize"; //TODO: remove after backward compatibility
    public static final String ACCOUNT_ID_FIELD = "account_id";
    public static final String DESCRIPTION_FIELD = "description";
    public static final String CREATED_BY_FIELD = "created_by";

    private final AuthTokenDao authDao;
    private final TokenService tokenService;

    public PublicAuthResource(AuthTokenDao authDao,
                              TokenService tokenService) {
        this.authDao = authDao;
        this.tokenService = tokenService;
    }

    @Path(API_AUTH_PATH)
    @Produces(APPLICATION_JSON)
    @GET
    public Response authenticateOld(@Auth String token) {
        return authDao.findUnRevokedAccount(token)
                .map(accountId -> ok(ImmutableMap.of("account_id", accountId)))
                .orElseGet(() -> UNAUTHORISED)
                .build();
    }

    @Path(FRONTEND_AUTH_PATH)
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @POST
    public Response createTokenForAccount(JsonNode payload) {
        return validateCreatePayload(payload)
                .map(errorMessage -> badRequestResponse(LOGGER, errorMessage))
                .orElseGet(() -> {
                    Tokens token = tokenService.issueTokens();
                    authDao.storeToken(token.getHashedToken(), randomUUID().toString(),
                            payload.get(ACCOUNT_ID_FIELD).asText(),
                            payload.get(DESCRIPTION_FIELD).asText(),
                            // FIXME removed following line and uncomment next after backward comp is not needed
                            payload.get(CREATED_BY_FIELD) != null ? payload.get(CREATED_BY_FIELD).asText() : "Not Stored");
                    // payload.get(CREATED_BY_FIELD).asText());
                    return ok(ImmutableMap.of("token", token.getApiKey())).build();
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
            return notFoundResponse(LOGGER, "Could not update token description");
        });
    }

    @Path(FRONTEND_AUTH_PATH + "/{accountId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @DELETE
    public Response revokeSingleToken(@PathParam("accountId") String accountId, JsonNode payload) {
        JsonNode jsonNode;
        if (payload == null || (jsonNode = payload.get("token_link")) == null) {
            return badRequestResponse(LOGGER, "Missing fields: [token_link]");
        }

        return authDao.revokeSingleToken(accountId, jsonNode.asText())
                .map(revokedDate -> ok(ImmutableMap.of("revoked", revokedDate)).build())
                .orElseGet(() -> notFoundResponse(LOGGER, "Could not revoke token"));
    }

    private Response withTokenLinkAndDescription(JsonNode requestPayload, BiFunction<String, String, Response> handler) {
        if (requestPayload == null) {
            return badRequestResponse(LOGGER, "Missing fields: [token_link, description]");
        }

        List<String> missingFieldsInRequestPayload = findMissingFieldsInRequestPayload(requestPayload, "token_link", "description");

        if (missingFieldsInRequestPayload.isEmpty()) {
            return handler.apply(requestPayload.get("token_link").asText(), requestPayload.get("description").asText());
        }
        return badRequestResponse(LOGGER, "Missing fields: [" + Joiner.on(", ").join(missingFieldsInRequestPayload) + "]");
    }

    private Optional<String> validateCreatePayload(JsonNode payload) {
        List<String> missingFieldsInRequestPayload = findMissingFieldsInRequestPayload(payload,
                ACCOUNT_ID_FIELD,
                DESCRIPTION_FIELD);
//                CREATED_BY_FIELD); // FIXME removed this comment after backward comp is not needed
        if (!missingFieldsInRequestPayload.isEmpty()) {
            return Optional.of("Missing fields: [" + Joiner.on(", ").join(missingFieldsInRequestPayload) + "]");
        }
        return Optional.empty();
    }

    private List<String> findMissingFieldsInRequestPayload(JsonNode requestPayload, String... expectedFieldsInRequestPayload) {
        if (requestPayload == null) {
            return asList(expectedFieldsInRequestPayload);
        }
        return Stream.of(expectedFieldsInRequestPayload)
                .filter(expectedKey -> requestPayload.get(expectedKey) == null)
                .collect(Collectors.toList());
    }
}
