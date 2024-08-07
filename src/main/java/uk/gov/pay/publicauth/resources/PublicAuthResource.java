package uk.gov.pay.publicauth.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.publicauth.auth.Token;
import uk.gov.pay.publicauth.exception.TokenNotFoundException;
import uk.gov.pay.publicauth.exception.ValidationException;
import uk.gov.pay.publicauth.model.AuthResponse;
import uk.gov.pay.publicauth.model.CreateTokenRequest;
import uk.gov.pay.publicauth.model.TokenHash;
import uk.gov.pay.publicauth.model.TokenLink;
import uk.gov.pay.publicauth.model.TokenResponse;
import uk.gov.pay.publicauth.model.TokenSource;
import uk.gov.pay.publicauth.model.TokenState;
import uk.gov.pay.publicauth.service.TokenService;

import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ok;
import static uk.gov.pay.publicauth.model.TokenSource.API;
import static uk.gov.pay.publicauth.model.TokenState.ACTIVE;

@Singleton
@Path("/")
@Tag(name = "Auth")
public class PublicAuthResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicAuthResource.class);

    private static final String TOKEN_LINK_FIELD = "token_link";
    private static final String TOKEN_FIELD = "token";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String REVOKED_DATE_FORMAT_PATTERN = "dd MMM yyyy";

    private final TokenService tokenService;

    public PublicAuthResource(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Path("/v1/api/auth")
    @Timed
    @Produces(APPLICATION_JSON)
    @GET
    @Operation(
            summary = "Look up the account ID for a token.",
            security = {@SecurityRequirement(name = "BearerAuth")},
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    public AuthResponse authenticate(@Parameter(hidden = true) @Auth Token token) {
        return tokenService.authenticate(TokenHash.of(token.getName()));
    }

    @Path("/v1/frontend/auth")
    @Timed
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @POST
    @Operation(
            summary = "Generate and return a new token for the given gateway account ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(example = "{" +
                            "    \"token\": \"api_live_6vra8l8mdtsikncr00etcg4ks3lktu88r8fa7k2re3f211cj8t3m1aeug5\"" +
                            "}"))),
                    @ApiResponse(responseCode = "422", description = "Invalid or missing required parameters")
            }
    )
    public Response createTokenForAccount(@NotNull @Valid CreateTokenRequest createTokenRequest) {
        String apiKey = tokenService.createTokenForAccount(createTokenRequest);
        return ok(Map.of("token", apiKey)).build();
    }

    @Path("/v1/frontend/auth/{accountId}/revoke-all")
    @Timed
    @DELETE
    public Response revokeTokensForAccount(@Parameter(example = "1") @PathParam("accountId") String accountId) {
        tokenService.revokeTokens(accountId);
        return ok().build();
    }

    @Path("/v1/frontend/auth/{accountId}")
    @Timed
    @Produces(APPLICATION_JSON)
    @GET
    @Operation(
            summary = "Retrieves generated tokens for gateway account.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schemaProperties = @SchemaProperty(name = "tokens",
                                    array = @ArraySchema(schema = @Schema(name = "tokens",
                                            implementation = TokenResponse.class, type = "array"))))),
                    @ApiResponse(responseCode = "422", description = "Invalid or missing required parameters")
            }
    )
    public Response getIssuedTokensForAccount(@Parameter(example = "1") @PathParam("accountId") String accountId,
                                              @Parameter(example = "REVOKED") @QueryParam("state") TokenState state,
                                              @Parameter(example = "API") @QueryParam("type") TokenSource type) {
        state = Optional.ofNullable(state).orElse(ACTIVE);
        type = Optional.ofNullable(type).orElse(API);
        List<TokenResponse> tokenResponses = tokenService.findTokensBy(accountId, state, type);
        return ok(Map.of("tokens", tokenResponses)).build();
    }

    @Path("/v1/frontend/auth")
    @Timed
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @PUT
    @Operation(
            summary = "Updates the description of an existing dev token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Token not found"),
                    @ApiResponse(responseCode = "422", description = "Invalid or missing missing parameters")
            }
    )
    public TokenResponse updateTokenDescription(
            @RequestBody(content = @Content(schema = @Schema(example = "{" +
                    "    \"token_link\": \"550e8400-e29b-41d4-a716-446655440000\"," +
                    "    \"description\": \"Description of the token\"" +
                    "}")))
                    JsonNode payload) throws ValidationException, TokenNotFoundException {

        validatePayloadHasFields(payload, TOKEN_LINK_FIELD, DESCRIPTION_FIELD);

        TokenLink tokenLink = TokenLink.of(payload.get(TOKEN_LINK_FIELD).asText());
        String description = payload.get(DESCRIPTION_FIELD).asText();

        return tokenService.updateTokenDescription(tokenLink, description);
    }

    @Path("/v1/frontend/auth/{accountId}")
    @Timed
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @DELETE
    @Operation(
            summary = "Revokes the supplied token for this account",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(example = "{" +
                                    "    \"revoked\": \"4 Apr 2022\"" +
                                    "}"))),
                    @ApiResponse(responseCode = "404", description = "Token not found")
            }
    )
    public Response revokeSingleToken(@Parameter(example = "1") @PathParam("accountId") String accountId,
                                      @RequestBody(content = @Content(schema = @Schema(example = "{" +
                                              "    \"token_link\": \"74813ca7-1829-4cad-bc0e-684a0288a308\"" +
                                              "}")))
                                              JsonNode payload) throws ValidationException, TokenNotFoundException {

        validatePayloadHasFields(payload, Collections.emptyList(), asList(TOKEN_LINK_FIELD, TOKEN_FIELD));

        if (payload.hasNonNull(TOKEN_FIELD)) {
            return tokenService.extractEncryptedTokenFrom(payload.get(TOKEN_FIELD).asText())
                    .map(token -> tokenService.revokeToken(accountId, TokenHash.of(token.getName())))
                    .map(this::buildRevokedTokenResponse)
                    .orElseThrow(() -> new TokenNotFoundException("Could not extract encrypted token while revoking token"));
        } else {
            TokenLink tokenLink = TokenLink.of(payload.get(TOKEN_LINK_FIELD).asText());
            ZonedDateTime revokedDate = tokenService.revokeToken(accountId, tokenLink);
            return buildRevokedTokenResponse(revokedDate);
        }

    }

    private Response buildRevokedTokenResponse(ZonedDateTime revokedDate) {
        LOGGER.info("revoked token on date {}", revokedDate);
        String formattedDate = DateTimeFormatter.ofPattern(REVOKED_DATE_FORMAT_PATTERN).format(revokedDate);
        return ok(Map.of("revoked", formattedDate)).build();
    }

    private void validatePayloadHasFields(JsonNode payload, String... expectedFields) throws ValidationException {
        validatePayloadHasFields(payload, asList(expectedFields), Collections.emptyList());
    }

    private void validatePayloadHasFields(JsonNode payload, List<String> expectedFields, List<String> atLeastOneOfTheseFieldsMustExist) throws ValidationException {
        if (payload == null) {
            throw new ValidationException("Body cannot be empty");
        }
        List<String> missingFields = expectedFields
                .stream()
                .filter(expectedKey -> !payload.has(expectedKey))
                .collect(Collectors.toList());
        if (!missingFields.isEmpty()) {
            throw new ValidationException("Missing fields: " + missingFields);
        }

        if (!atLeastOneOfTheseFieldsMustExist.isEmpty() && atLeastOneOfTheseFieldsMustExist
                .stream()
                .noneMatch(payload::has)) {
            throw new ValidationException("At least one of these fields must be present: " + atLeastOneOfTheseFieldsMustExist);
        }
    }
}
