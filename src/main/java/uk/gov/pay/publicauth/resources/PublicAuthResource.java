package uk.gov.pay.publicauth.resources;

import com.google.common.collect.ImmutableMap;
import uk.gov.pay.publicauth.dao.AuthTokenDao;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/")
public class PublicAuthResource {

    public static final String AUTH_PATH = "v1/auth";
    private AuthTokenDao authDao;

    private final static String BEARER_PREFIX = "Bearer ";

    public PublicAuthResource(AuthTokenDao authDao) {
        this.authDao = authDao;
    }

    @Path(AUTH_PATH)
    @Produces(APPLICATION_JSON)
    @GET
    public Response authenticate(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearerToken) {
        String tokenId = bearerToken.substring(BEARER_PREFIX.length());
        Optional<String> account = authDao.findAccount(tokenId);
        return account.map(
                        accountId -> Response.ok(ImmutableMap.of("account_id", accountId)))
                .orElseGet(
                        () -> Response.status(Response.Status.UNAUTHORIZED))
                .build();
    }

}
