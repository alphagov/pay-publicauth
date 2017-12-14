package uk.gov.pay.publicauth.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.auth.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.auth.Token;
import uk.gov.pay.publicauth.dao.AuthTokenDao;
import uk.gov.pay.publicauth.exception.TokenNotFoundException;
import uk.gov.pay.publicauth.exception.ValidationException;
import uk.gov.pay.publicauth.model.TokenPaymentType;
import uk.gov.pay.publicauth.model.TokenStateFilterParam;
import uk.gov.pay.publicauth.model.Tokens;
import uk.gov.pay.publicauth.service.TokenService;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ResponseBuilder;
import static javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static uk.gov.pay.publicauth.model.TokenPaymentType.CARD;
import static uk.gov.pay.publicauth.model.TokenPaymentType.valueOf;
import static uk.gov.pay.publicauth.model.TokenStateFilterParam.ACTIVE;
import static uk.gov.pay.publicauth.util.ResponseUtil.badRequestResponse;
import static uk.gov.pay.publicauth.util.ResponseUtil.notFoundResponse;

@Singleton
@Path("/")
public class PublicAuthResource {

    public static final String API_VERSION_PATH = "/v1";
    public static final String ACCOUNT_ID_FIELD = "account_id";
    public static final String DESCRIPTION_FIELD = "description";
    public static final String CREATED_BY_FIELD = "created_by";
    public static final String TOKEN_TYPE_FIELD = "token_type";
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicAuthResource.class);
    private static final ResponseBuilder UNAUTHORISED = status(Status.UNAUTHORIZED);
    private static final String API_AUTH_PATH = API_VERSION_PATH + "/api/auth";
    private static final String FRONTEND_AUTH_PATH = API_VERSION_PATH + "/frontend/auth";
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
    public Response authenticate(@Auth Token token) {
        return authDao.findUnRevokedAccount(token.getName())
                .map(tokenInfo -> ok(ImmutableMap.of(
                        "account_id", tokenInfo.get("account_id"),
                        "token_type", tokenInfo.get("token_type").toString())))
                .orElse(UNAUTHORISED)
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
                    TokenPaymentType tokenPaymentType =
                            Optional.ofNullable(payload.get(TOKEN_TYPE_FIELD))
                                    .map(tokenType -> valueOf(tokenType.asText()))
                                    .orElse(CARD);
                    String tokenLink = randomUUID().toString();
                    authDao.storeToken(token.getHashedToken(),
                            tokenLink,
                            payload.get(ACCOUNT_ID_FIELD).asText(),
                            payload.get(DESCRIPTION_FIELD).asText(),
                            payload.get(CREATED_BY_FIELD).asText(),
                            tokenPaymentType);
                    LOGGER.info("Created token with token_link {} ", tokenLink);
                    return ok(ImmutableMap.of("token", token.getApiKey())).build();
                });
    }

    @Path(FRONTEND_AUTH_PATH + "/{accountId}")
    @Produces(APPLICATION_JSON)
    @GET
    public Response getIssuedTokensForAccount(@PathParam("accountId") String accountId, @QueryParam("state") TokenStateFilterParam state) {
        state = Optional.ofNullable(state).orElse(ACTIVE);
        List<Map<String, Object>> tokensWithoutNullRevoked = authDao.findTokensWithState(accountId, state);
        return ok(ImmutableMap.of("tokens", tokensWithoutNullRevoked)).build();
    }

    @Path(FRONTEND_AUTH_PATH)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @PUT
    public Response updateTokenDescription(JsonNode payload) throws ValidationException, TokenNotFoundException {

        validatePayloadHasFields(payload, "token_link", "description");

        String tokenLink = payload.get("token_link").asText();
        String description = payload.get("description").asText();

        if (authDao.updateTokenDescription(tokenLink, description)) {
            LOGGER.info("Updated description of token with token_link {}", tokenLink);
            return authDao.findTokenByTokenLink(tokenLink)
                    .map(token -> ok(token).build())
                    .orElseThrow(() -> new TokenNotFoundException("Could not update  description of token with token_link " + tokenLink));
        }

        LOGGER.error("Could not update  description of token with token_link " + tokenLink);
        throw new TokenNotFoundException("Could not update description of token with token_link " + tokenLink);
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
        String tokenLink = jsonNode.asText();
        return authDao.revokeSingleToken(accountId, tokenLink)
                .map(revokedDate -> {
                    LOGGER.info("revoked with token_link {} on date {}", tokenLink, revokedDate);
                    return ok(ImmutableMap.of("revoked", revokedDate)).build();
                })
                .orElseGet(() -> notFoundResponse(LOGGER, "Could not revoke token with token_link " + tokenLink));
    }

    private Optional<String> validateCreatePayload(JsonNode payload) {
        List<String> missingFieldsInRequestPayload = findMissingFieldsInRequestPayload(payload, ACCOUNT_ID_FIELD, DESCRIPTION_FIELD, CREATED_BY_FIELD);
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

    private void validatePayloadHasFields(JsonNode payload, String... expectedFields) throws ValidationException {
        List<String> missingFields;
        if (payload == null) {
            missingFields = asList(expectedFields);
        } else {
            missingFields = Stream.of(expectedFields)
                    .filter(expectedKey -> payload.get(expectedKey) == null)
                    .collect(Collectors.toList());
        }
        if (!missingFields.isEmpty()) {
            throw new ValidationException("Missing fields: [" + Joiner.on(", ").join(missingFields) + "]");
        }
    }
}
